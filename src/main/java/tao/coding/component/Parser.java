package tao.coding.component;

import lombok.extern.slf4j.Slf4j;
import tao.coding.entity.Chapter;
import tao.coding.entity.Content;
import tao.coding.entity.RelayNode;
import tao.coding.util.CheckedExceptionFucker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static tao.coding.component.Task.taskExecutor;

/**
 * 组件：从选择的元素中进一步解析想要的内容
 */
@FunctionalInterface
public interface Parser<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T source) throws Exception {
        return parse(source);
    }

    CompletableFuture<R> parse(T source) throws Exception;

    // 组件名
    static String name() {
        return "「析」";
    }

    @Slf4j
    class Parsers {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Parser.name());
        }

        // bid解析器
        public static Parser<String, String> bidParser() {
            // bid 无需额外解析，只是过个流程
            return source -> CompletableFuture.completedFuture(source)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行解析bid内容操作", Parser.name()), taskExecutor());
        }

        // 章节列表解析器
        public static Parser<String, List<Chapter.Chapter4Read>> chapterParser() {
            return chapterSource -> CompletableFuture.completedFuture(chapterSource)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行解析章节列表操作", Parser.name()), taskExecutor())
                    .thenApplyAsync(CheckedExceptionFucker::readTree, taskExecutor())// 返回根节点
                    .thenApplyAsync(RelayNode::of, taskExecutor())// 转换为中继节点
                    .thenApplyAsync(RelayNode::map2Chapter4ReadList, taskExecutor());// 转换为 Chapter4ReadList
        }

        // 章节内容解析器
        public static Parser<Chapter.Chapter4Parse, Chapter.Chapter4Decode> contentParser() {
            // 构建为 Chapter4Decode
            return chapter4Parse -> CompletableFuture.completedFuture(chapter4Parse)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行解析章节内容操作", Parser.name()), taskExecutor())
                    .thenApplyAsync(Chapter.Chapter4Parse::jsonCiphertext, taskExecutor())
                    .thenApplyAsync(jsonCiphertext -> CheckedExceptionFucker.readValue(jsonCiphertext, Content.class), taskExecutor())
                    .thenApplyAsync(content -> new Chapter.Chapter4Decode(chapter4Parse.bookName(), chapter4Parse.chapterName(), chapter4Parse.chapterOrdid(), content.ChapterContent()), taskExecutor());
        }
    }
}