package tao.coding.util;

import tao.coding.flow.FlowEngine;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 书籍缓存类：用于记录一些书籍在下载过程中的所需数据
public class BookCache {
    // 默认文件路径 e.g. D:/斗破苍穹/1.txt or D:/斗破苍穹/斗破苍穹.txt
    private static final String FILE_PATH_FORMATTER = "%s/%s.txt";
    // 文件通道缓存
    private static final Map<String, FileChannel> FILE_CHANNEL_MAP = new ConcurrentHashMap<>();

    // 获取文件夹路径
    public static Path getFolderPath(String bookName) {
        try {
            return Paths.get(String.format(FlowEngine.FOLDER_FORMATTER, bookName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取文件路径
    public static Path getFilePath(String bookName, Integer chapterOrdid) {
        try {
            return Paths.get(String.format(FILE_PATH_FORMATTER, getFolderPath(bookName), chapterOrdid));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取文件通道
    public static FileChannel getFileChannel(String bookName) {
        return FILE_CHANNEL_MAP.computeIfAbsent(bookName, bkName -> {
            try {
                // 文件夹路径
                var folderPath = getFolderPath(bkName);
                // 合并后的目标文件路径 e.g. D:/斗破苍穹/斗破苍穹.txt
                var targetFilePath = Paths.get(FILE_PATH_FORMATTER.formatted(folderPath, bkName));
                // 使用 RandomAccessFile 预设文件大小，与 FileChannel 共享文件描述符，此处无需关闭
                var raf = new RandomAccessFile(targetFilePath.toFile(), "rw");
                // 计算合并后的文件总长度（字节）
                var totalLength = Files.list(folderPath)
                        .map(path -> {
                            try {
                                return Files.size(path);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .reduce(0L, Long::sum);
                // 预设置总文件大小，避免重复扩容，提升写入时性能
                raf.setLength(totalLength);
                return raf.getChannel();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 删除文件通道
    public static void removeFileChannel(String bookName) {
        FILE_CHANNEL_MAP.computeIfPresent(bookName, (_, v) -> {
            // 关闭文件通道
            try (v) {
                // 删除文件通道缓存
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
