package tao.coding.component;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import tao.coding.entity.Chapter;

import java.util.concurrent.CompletableFuture;

import static tao.coding.component.Task.taskExecutor;

/**
 * 组件：从响应文本中挑选所需的元素
 */
@FunctionalInterface
public interface Selector<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T doc) throws Exception {
        return select(doc);
    }

    CompletableFuture<R> select(T doc) throws Exception;

    // 组件名
    static String name() {
        return "「择」";
    }

    @Slf4j
    class Selectors {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Selector.name());
        }

        // bid元素选择器
        public static Selector<String, String> bidSelector() {
            // bid元素所在位置:  <span class="btn js-addShelf disable" data-bid="53258" data-clog="shelf-shelf$$bid=53258">+书架</span>
            final var bidXpath = "/html/body/div[1]/div[3]/div/div[4]/div/span[2]";
            return bidDoc -> CompletableFuture.completedFuture(bidDoc)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行选择bid元素操作", Selector.name()), taskExecutor())
                    .thenApplyAsync(Jsoup::parse, taskExecutor())
                    .thenApplyAsync(doc -> doc.selectXpath(bidXpath), taskExecutor())
                    .thenApplyAsync(Elements::getFirst, taskExecutor())
                    .thenApplyAsync(node -> node.attr("data-bid"), taskExecutor());
        }

        // 章节列表元素选择器
        public static Selector<String, String> chapterSelector() {
            final var chapterXpath = "/html/body/i[5]";
            return chapterDoc -> CompletableFuture.completedFuture(chapterDoc)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行选择章节列表元素操作", Selector.name()), taskExecutor())
                    .thenApplyAsync(Jsoup::parse, taskExecutor())
                    .thenApplyAsync(doc -> doc.selectXpath(chapterXpath), taskExecutor())
                    .thenApplyAsync(Elements::text, taskExecutor());
        }

        // 章节内容元素选择器
        public static Selector<Chapter.Chapter4Select, Chapter.Chapter4Parse> contentSelector() {
            // map 2 Chapter4Parse
            return chapter4Select -> CompletableFuture.completedFuture(chapter4Select)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行选择章节内容元素操作", Selector.name()), taskExecutor())
                    .thenApplyAsync(c4s -> new Chapter.Chapter4Parse(c4s.bookName(), c4s.chapterName(), c4s.chapterOrdid(), c4s.jsonCiphertext()), taskExecutor());
        }
    }
}