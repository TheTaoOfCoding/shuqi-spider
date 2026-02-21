package tao.coding.component;

import lombok.extern.slf4j.Slf4j;
import tao.coding.entity.Chapter;
import tao.coding.util.BookCache;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

import static tao.coding.component.Task.taskExecutor;

/**
 * 组件：保存章节内容，将排版后的章节内容写入文件，每章为一个文件
 */
@FunctionalInterface
public interface Writer extends Task<Chapter.Chapter4Write, Chapter.Chapter4Merge> {
    @Override
    default CompletableFuture<Chapter.Chapter4Merge> execute(Chapter.Chapter4Write chapter4Write) throws Exception {
        return write(chapter4Write);
    }

    CompletableFuture<Chapter.Chapter4Merge> write(Chapter.Chapter4Write chapter4Write) throws Exception;

    // 组件名
    static String name() {
        return "「录」";
    }

    static CompletableFuture<Chapter.Chapter4Merge> write0(Chapter.Chapter4Write chapter) {
        // 书名
        var bookName = chapter.bookName();
        // chapterOrdid 章节序号 为 1 - N 的连续自然数
        var chapterOrdid = chapter.chapterOrdid();
        // 文件夹路径
        var folderPath = BookCache.getFolderPath(bookName);
        // 文件路径
        var filePath = BookCache.getFilePath(bookName, chapterOrdid);
        try {
            /*
             * 此处为并发环境, double check 以创建文件夹，实则是多余操作
             * Files.createDirectories(folderPath) 行为：不存在则创建，存在时则不作任何操作，故为线程安全的操作
             * 多余的判断行为姑且就算做是用于提醒注意当前操作环境吧
             */
            if (Files.notExists(folderPath)) {
                // 使用 String 作为锁对象时，intern 确保使用同一对象
                synchronized (bookName.intern()) {
                    if (Files.notExists(folderPath)) Files.createDirectories(folderPath);
                }
            }
            // 执行文件写入
            Files.writeString(filePath, chapter.chapterContext(), StandardCharsets.UTF_8);
            // 写入完成后打开只读文件通道
            var fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
            var chapter4Merge = new Chapter.Chapter4Merge(bookName, chapter.chapterOrdid(), filePath, fileChannel);
            return CompletableFuture.completedFuture(chapter4Merge);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Slf4j
    class Writers {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Writer.name());
        }

        // 将章节内容打印在控制台，调试时用
        public static Writer consoleWriter() {
            final var part = "-".repeat(15);
            return chapter4Write -> CompletableFuture.completedFuture(chapter4Write)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行文件写入操作[控制台]", Writer.name()), taskExecutor())
                    .thenApplyAsync(c4w -> String.format("%s\t%s\t%s\n%s", part, c4w.chapterName(), part, c4w.chapterContext()), taskExecutor())
                    .whenCompleteAsync((chapterContent, _) -> IO.println(chapterContent), taskExecutor())
                    .thenApplyAsync(_ -> null, taskExecutor());
        }

        // 将章节内容写入文件
        public static Writer fileWriter() {
            return chapter4Write -> CompletableFuture.completedFuture(chapter4Write)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行文件写入操作[文件系统]", Writer.name()), taskExecutor())
                    .thenComposeAsync(Writer::write0, taskExecutor())
                    .whenCompleteAsync((chapter4Merge, _) -> log.info("{} - 文件写入操作[文件系统]完成 path => {}", Writer.name(), chapter4Merge.filePath()), taskExecutor());
        }
    }
}