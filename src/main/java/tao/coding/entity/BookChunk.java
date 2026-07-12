package tao.coding.entity;

import lombok.extern.slf4j.Slf4j;
import tao.coding.component.Task;
import tao.coding.flow.FlowEngine;
import tao.coding.util.BookCache;
import tao.coding.util.IOForkJoinTask;
import tao.coding.util.ScopedExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IOForkJoinTask 实际子类
 *
 * @param sources    要处理的资源
 * @param startIndex 起始索引
 * @param endIndex   结束索引
 * @param capacity   可以处理的资源数
 * @param executor   线程池
 */
@Slf4j
public record BookChunk(List<Chapter.Chapter4Merge> sources, Integer startIndex, Integer endIndex, Integer capacity, Executor executor) implements IOForkJoinTask<BookChunk> {

    // 从 sources 构造
    public static BookChunk of(List<Chapter.Chapter4Merge> sources) {
        var orderIds = sources.stream().map(Chapter.Chapter4Merge::chapterOrdid).toList();
        return new BookChunk(sources, orderIds.getFirst(), orderIds.getLast(), FlowEngine.DEFAULT_CAPACITY, Task.taskExecutor());
    }

    public String name() {
        return "「镇坛木」";
    }

    @Override
    public CompletableFuture<Result> doCompute() {
        var name = name();
        log.info("{} - 准备合并 [{} ~ {}]", name, startIndex, endIndex);

        // 书籍名称
        var bookName = ScopedExecutor.ScopedExecutors.KEY.get();
        // 获取目标文件通道
        var targetFileChannel = BookCache.getFileChannel(bookName);
        var bytesCounter = new AtomicLong(0L);
        sources.stream()
                .skip(startIndex - 1) // [startIndex ~ endIndex]为从1开始的连续自然数，故此处取需 - 1
                .limit(endIndex - startIndex + 1)
                .forEach(chapter4Merge -> {
                    var skip = chapter4Merge.skip();
                    try (var sourceChannel = chapter4Merge.fileChannel()) {
                        var byteSize = targetFileChannel.transferFrom(sourceChannel, skip, sourceChannel.size());// 零拷贝
                        bytesCounter.addAndGet(byteSize);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var successful = endIndex - startIndex + 1;
        var byteSize = bytesCounter.get();
        log.info("{} - 合并完成 [{} ~ {} : {}, {}]", name, startIndex, endIndex, successful, byteSize);
        var result = new Result(successful, byteSize);
        return CompletableFuture.completedFuture(result);
    }

    // 二分法拆分任务
    @Override
    public BookChunk[] doFork() {
        var medianIndex = (startIndex + endIndex) >> 1;// = (endIndex - startIndex) / 2 + startIndex
        var left = new BookChunk(sources, startIndex, medianIndex, capacity, executor);
        var right = new BookChunk(sources, medianIndex + 1, endIndex, capacity, executor);
        log.info("{} - 执行拆分 left[{} ~ {}],right[{} ~ {}]", name(), left.startIndex, left.endIndex, right.startIndex, right.endIndex);
        return new BookChunk[]{left, right};
    }
}