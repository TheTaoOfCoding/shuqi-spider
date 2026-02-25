package tao.coding.component;

import lombok.extern.slf4j.Slf4j;
import tao.coding.entity.Chapter;
import tao.coding.flow.FlowEngine;
import tao.coding.util.Assert;
import tao.coding.util.ContextPool;
import tao.coding.util.FailFastHandler;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import static tao.coding.component.Task.taskExecutor;

/**
 * 组件：解密加密的章节内容
 */
@FunctionalInterface
public interface Decoder extends Task<Chapter.Chapter4Decode, Chapter.Chapter4Format> {

    @Override
    default CompletableFuture<Chapter.Chapter4Format> execute(Chapter.Chapter4Decode chapter4Decode) throws Exception {
        return decode(chapter4Decode);
    }

    CompletableFuture<Chapter.Chapter4Format> decode(Chapter.Chapter4Decode chapter4Decode) throws Exception;

    // js 解密
    static String withJsDecode(String ciphertext) {
        // 调用 js引擎池 解密章节内容
        var context = ContextPool.acquire();
        var plaintext = context.getBindings("js")
                .getMember("_decode")
                .execute(ciphertext)
                .asString();
        // 释放至 js引擎池
        ContextPool.release(context);
        return plaintext;
    }

    // java 本地实现解密（随js脚本更迭）
    static String withNativeDecode(String ciphertext) {
        StringBuilder transformed = new StringBuilder();
        for (char c : ciphertext.toCharArray()) {
            if (Character.isLetter(c)) {
                int e = c / 97;
                char lowerChar = Character.toLowerCase(c);
                int i = (lowerChar - 83) % 26;
                if (i == 0) i = 26;
                char decodedChar = (char) (i + (e == 0 ? 64 : 96));
                transformed.append(decodedChar);
            } else {
                transformed.append(c);
            }
        }
        String step1 = transformed.toString();
        String cleaned = step1.replaceAll("[^A-Za-z0-9+/=]", "");
        byte[] decodedBytes = Base64.getDecoder().decode(cleaned);
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < decodedBytes.length) {
            int b = decodedBytes[index] & 0xFF;
            if (b < 128) {
                result.append((char) b);
                index++;
            } else if (b > 191 && b < 224) {
                int b2 = decodedBytes[index + 1] & 0xFF;
                result.append((char) ((31 & b) << 6 | (63 & b2)));
                index += 2;
            } else {
                int b2 = decodedBytes[index + 1] & 0xFF;
                int b3 = decodedBytes[index + 2] & 0xFF;
                result.append((char) ((15 & b) << 12 | (63 & b2) << 6 | (63 & b3)));
                index += 3;
            }
        }
        return result.toString();
    }

    // 组件名
    static String name() {
        return "「译」";
    }

    @Slf4j
    class Decoders {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Decoder.name());
        }

        public static Decoder contentDecoder() {
            return chapter4Decode -> CompletableFuture.completedFuture(chapter4Decode)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行解密操作", Decoder.name()), taskExecutor())
                    .thenApplyAsync(Chapter.Chapter4Decode::ciphertext, taskExecutor())
                    .whenCompleteAsync((ciphertext, _) -> Assert.isTrue(ciphertext, Assert::isNotNull, () -> new NullPointerException("无法下载VIP章节，如已开通VIP账号，请自行添加VIP权限校验。")), taskExecutor())
                    .exceptionallyAsync(FailFastHandler::handle, taskExecutor())
                    .thenApplyAsync(FlowEngine.USE_NATIVE ? Decoder::withNativeDecode : Decoder::withJsDecode, taskExecutor())// 根据配置选择解密方式
                    .thenApplyAsync(unformattedChapterContent -> new Chapter.Chapter4Format(chapter4Decode.bookName(), chapter4Decode.chapterName(), chapter4Decode.chapterOrdid(), unformattedChapterContent), taskExecutor());
        }
    }
}