package tao.coding.component;

import tao.coding.util.Assert;
import tao.coding.util.RateLimiter;
import tao.coding.util.ScopedExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/*
 * 抽象通用节点
 */
@SuppressWarnings("all")
@FunctionalInterface
public interface Task<T, R> extends Function<T, CompletableFuture<R>> {

    CompletableFuture<R> execute(T param) throws Exception;

    @Override
    default CompletableFuture<R> apply(T param) {
        try {
            return execute(param);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 高阶函数：利用函数式编程的函数组合特性，来组装两个任务
     * 同步调用链
     */
    default <V> Task<T, V> then(Task<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenCompose(next);
    }

    /*
     * 高阶函数：利用函数式编程的函数组合特性，来组装两个任务
     * 异步调用链
     */
    default <V> Task<T, V> thenAsync(Task<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenComposeAsync(next, taskExecutor());
    }

    /*
     * 一致性任务
     */
    static <T> Task<T, T> identity() {
        return CompletableFuture::completedFuture;
    }

    /*
     * 空任务
     */
    static <T, R> Task<T, R> empty() {
        return _ -> CompletableFuture.completedFuture((R) null);
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Function<List<T>, List<T>> before, Task<? super T, R> task) {
        return parallelTask(before, task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Task<? super T, R> task) {
        return parallelTask(Function.identity(), task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Task<? super T, R> task, Function<List<R>, List<R>> after) {
        return parallelTask(Function.identity(), task, after);
    }

    /*
     * 并行任务（模板方法：算法骨架已然固定）
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Function<List<T>, List<T>> before, Task<? super T, R> task, Function<List<R>, List<R>> after) {
        Assert.isTrue(before, Assert::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        Assert.isTrue(task, Assert::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        Assert.isTrue(after, Assert::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        final var atomicReference = new AtomicReference<CompletableFuture[]>();
        return items -> CompletableFuture.completedFuture(items)
                .thenApplyAsync(before, taskExecutor()) // 参数前置处理
                .thenApplyAsync(list -> atomicReference.updateAndGet(_ -> list.stream().map(task).toArray(CompletableFuture[]::new)), taskExecutor()) // 并行执行任务
                .thenComposeAsync(CompletableFuture::allOf, taskExecutor()) // 等待所有任务完成
                .thenApplyAsync(_ -> Arrays.stream(atomicReference.get()).map(CompletableFuture<R>::join).toList(), taskExecutor()) // 汇总任务结果
                .thenApplyAsync(after, taskExecutor()); // 返回值后置处理
    }

    /*
     * 流控任务专员（装饰器模式）
     */
    static <T, R> Task<T, ? extends R> withRateLimit(Task<? super T, R> innerTask, long delay) {
        Assert.isTrue(innerTask, Assert::isNotNull, () -> new NullPointerException("The only way to do great work is to love what you do. — Steve Jobs"));
        return t -> CompletableFuture.completedFuture(t)
                .thenApplyAsync(RateLimiter::acquire, taskExecutor()) // 执行任务前获取信号量
                .thenComposeAsync(innerTask, CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS, taskExecutor()))// 使用包装后带延时的线程池
                .whenCompleteAsync(RateLimiter::release, taskExecutor()); // 任务结束时释放信号量
    }

    /*
     * 任务专用线程池（使用经过包装的虚拟线程池，从当前上下文中获取 ScopedValue 并绑定至新开启的虚拟线程）
     */
    static ScopedExecutor taskExecutor() {
        return ScopedExecutor.ScopedExecutors.newScopedExecutor();
    }
}