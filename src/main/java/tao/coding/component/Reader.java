package tao.coding.component;

import lombok.extern.slf4j.Slf4j;
import tao.coding.entity.Chapter;
import tao.coding.entity.Tao;
import tao.coding.flow.FlowEngine;
import tao.coding.util.RateLimiter;
import tao.coding.util.ScopedExecutor;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static tao.coding.component.Task.taskExecutor;

/**
 * 组件：发送请求，获取响应文本
 */
@FunctionalInterface
public interface Reader<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T param) throws Exception {
        return read(param);
    }

    CompletableFuture<R> read(T param) throws Exception;

    // 发送请求,获取响应文本
    static CompletableFuture<String> read0(String uri) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                //.header("cookie", "") // 此处添加 VIP账号权限
                .build();

        return FlowEngine.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(HttpResponse::body, taskExecutor());
    }

    // 组件名
    static String name() {
        return "「载」";
    }

    @Slf4j
    class Readers {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Reader.name());
        }

        // 获取bid的http请求器
        public static Reader<Tao, String> bidReader() {
            // 获取BID的请求地址
            final var bidUriFormatter = "https://www.shuqi.com/search?keyword=%s&page=1";
            return _ -> CompletableFuture.completedFuture(ScopedExecutor.ScopedExecutors.KEY.get())
                    .thenApplyAsync(bidUriFormatter::formatted, taskExecutor())
                    .whenCompleteAsync((bidUri, _) -> log.info("{} - 执行获取bid操作 url => {}", Reader.name(), bidUri), taskExecutor())
                    .thenComposeAsync(Reader::read0, taskExecutor());
        }

        // 获取章节列表的http请求器
        public static Reader<String, String> chapterReader() {
            // 获取章节列表的请求地址
            final var chapterUriFormatter = "https://www.shuqi.com/reader?bid=%s";
            return bid -> CompletableFuture.completedFuture(bid)
                    .thenApplyAsync(chapterUriFormatter::formatted, taskExecutor())
                    .whenCompleteAsync((chapterUri, _) -> log.info("{} - 执行获取章节列表操作 url => {}", Reader.name(), chapterUri), taskExecutor())
                    .thenComposeAsync(Reader::read0, taskExecutor());
        }

        // 获取章节内容的http请求器
        public static Reader<Chapter.Chapter4Read, Chapter.Chapter4Select> contentReader() {
            // 获取章节内容请求地址
            final var contentUriFormatter = "https://c13.shuqireader.com/pcapi/chapter/contentfree/%s";
            return chapter4Read -> CompletableFuture.completedFuture(chapter4Read)
                    .thenApplyAsync(Chapter.Chapter4Read::contUrlSuffix, taskExecutor())
                    .thenApplyAsync(contentUriFormatter::formatted, taskExecutor())
                    .whenCompleteAsync((contentUri, _) -> log.info("{} - 执行获取章节内容操作 url => {}", Reader.name(), contentUri), taskExecutor())
                    .thenComposeAsync(Task.<String, String>withRateLimit(Reader::read0, RateLimiter.DELAY), taskExecutor())
                    .thenApplyAsync(jsonStr -> new Chapter.Chapter4Select(chapter4Read.bookName(), chapter4Read.chapterName(), chapter4Read.chapterOrdid(), jsonStr), taskExecutor());
        }
    }
}