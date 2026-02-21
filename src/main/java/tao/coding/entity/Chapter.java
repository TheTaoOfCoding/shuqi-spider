package tao.coding.entity;

import tao.coding.flow.FlowEngine;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

// 章节内容
public class Chapter {
    // 下载时
    public record Chapter4Read(String bookName, String chapterName, Integer chapterOrdid, String contUrlSuffix) {
    }

    // 选择时
    public record Chapter4Select(String bookName, String chapterName, Integer chapterOrdid, String jsonCiphertext) {
    }

    // 解析时
    public record Chapter4Parse(String bookName, String chapterName, Integer chapterOrdid, String jsonCiphertext) {
    }

    // 解密时
    public record Chapter4Decode(String bookName, String chapterName, Integer chapterOrdid, String ciphertext) {
    }

    // 排版时
    public record Chapter4Format(String bookName, String chapterName, Integer chapterOrdid,
                                 String unformattedChapterContent) {
    }

    // 保存时
    public record Chapter4Write(String bookName, String chapterName, Integer chapterOrdid, String chapterContext) {
    }

    // 文件合并时
    public record Chapter4Merge(String bookName, Integer chapterOrdid, Path filePath, FileChannel fileChannel, Long skip) {
        public Chapter4Merge(Chapter4Merge chapter4Merge, Long skip) {
            this(chapter4Merge.bookName, chapter4Merge.chapterOrdid, chapter4Merge.filePath, chapter4Merge.fileChannel, skip);
        }

        public Chapter4Merge(String bookName, Integer orderId, Path filePath, FileChannel fileChannel) {
            this(bookName, orderId, filePath, fileChannel, -1L);
        }

        public static Chapter4Merge of(Chapter4Merge chapter4Merge, AtomicLong atomicLong) {
            try {
                // 设置每章的跳过字节数 skip : atoLong.getAndAdd(size)
                var size = chapter4Merge.fileChannel().size();
                return new Chapter.Chapter4Merge(chapter4Merge, atomicLong.getAndAdd(size));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 清理时
    public record Chapter4Clean(String bookName, List<Path> paths) {
        public static Chapter4Clean of(List<Chapter.Chapter4Merge> chapter4Merges) {
            var bookName = chapter4Merges.getFirst().bookName();
            var paths = chapter4Merges.stream().map(Chapter4Merge::filePath).toList();
            return new Chapter.Chapter4Clean(bookName, paths);
        }

        public static boolean needDelete(Path unused) {
            return FlowEngine.NEED_DELETE;
        }
    }
}