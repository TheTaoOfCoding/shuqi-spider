package tao.coding.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 用于IO密集型任务分治的工具类
 */
public interface IOForkJoinTask<T extends IOForkJoinTask<T>> {

    Logger log = LoggerFactory.getLogger(IOForkJoinTask.class);

    static String name() {
        return "「天蓬尺」";
    }

    // 线程池
    Executor executor();

    // 起始索引
    Integer startIndex();

    // 结束索引
    Integer endIndex();

    // 可以处理的资源数量
    Integer capacity();

    // 是否需要拆分（模板方法模式，定义流程算法骨架）
    default Boolean needFork() {
        return (endIndex() - startIndex() + 1) > capacity();
    }

    // 要在线程内执行的任务的起始点（模板方法模式，定义流程算法骨架）
    default Result compute() {
        return needFork() ? join(fork()) : doCompute();
    }

    // 执行具体的任务操作由子类实现
    Result doCompute();

    // 任务拆分
    @SuppressWarnings("unchecked")
    default CompletableFuture<Result>[] fork() {
        return Arrays.stream(doFork())
                .map(task -> CompletableFuture.supplyAsync(task::compute, executor())) // 将子任务提交至线程池
                .toArray(CompletableFuture[]::new);
    }

    // 具体拆分算法由子类实现
    T[] doFork();

    // 当前任务阻塞等待子任务的返回结果
    @SuppressWarnings("unchecked")
    default Result join(CompletableFuture<Result>... futures) {
        // 返回值用以计算任务成功数量
        log.info("{} - 等待子任务返回 ...", IOForkJoinTask.name());
        // 合并子任务返回结果
        var result = CompletableFuture.allOf(futures) // 等待所有子任务完成
                .thenApplyAsync(_ -> Arrays.stream(futures).map(CompletableFuture::join).reduce(Result.ZERO, Result::reduce), executor()) // 汇总返回子任务结果
                .join();
        log.info("{} - 返回结果:{}", IOForkJoinTask.name(), result);
        return result;
    }

    /**
     * 返回的结果封装
     *
     * @param successful 成功处理的资源数量
     * @param byteSize   成功处理的字节数
     */
    record Result(Integer successful, Long byteSize) {
        public static final Result ZERO = new Result(0, 0L);

        public static Result reduce(Result left, Result right) {
            return new Result(left.successful + right.successful, left.byteSize + right.byteSize);
        }
    }
}