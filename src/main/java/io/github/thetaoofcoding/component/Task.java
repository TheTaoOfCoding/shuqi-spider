package io.github.thetaoofcoding.component;

import io.github.thetaoofcoding.util.Assert;
import io.github.thetaoofcoding.util.Assert.Predicates;
import io.github.thetaoofcoding.util.RateLimiter;
import io.github.thetaoofcoding.util.ScopedExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

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
            return CompletableFuture.failedFuture(e);
        }
    }

    /*
     * 任务组装，同步调用链
     */
    default <V> Task<T, V> then(Task<? super R, V> next) {
        Assert.isTrue(next, Predicates::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenCompose(next);
    }

    /*
     * 任务组装，异步调用链
     */
    default <V> Task<T, V> thenAsync(Task<? super R, V> next) {
        Assert.isTrue(next, Predicates::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenComposeAsync(next, taskExecutor());
    }

    /*
     * 重试任务
     */
    default Task<T, R> retry() {
        return retry(1L, 2L);
    }

    /*
     * 重试任务（延时后重复一次）
     */
    default Task<T, R> retry(long delay) {
        Assert.isTrue(delay, 0L, Predicates::isOrderGt, () -> new IllegalArgumentException("Whether you think you can or you think you can't, you're right. – Henry Ford"));
        return t -> execute(t).exceptionallyComposeAsync(_ -> apply(t), delayedTaskExecutor(delay));
    }

    /*
     * 重试任务（延时后重复多次）
     */
    default Task<T, R> retry(long delay, long times) {
        Assert.isTrue(times, 1L, Predicates::isOrderGe, () -> new IllegalArgumentException("Whether you think you can or you think you can't, you're right. – Henry Ford"));
        if (times == 1) return retry(delay);
        return retry(delay, times - 1).retry(delay + 1);
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
        return _ -> CompletableFuture.<R>completedFuture(null);
    }

    /**
     * 方法重载自 {@link Task#withParallel(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> withParallel(Function<List<T>, List<T>> before, Task<? super T, R> task) {
        return withParallel(before, task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#withParallel(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> withParallel(Task<? super T, R> task) {
        return withParallel(Function.identity(), task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#withParallel(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> withParallel(Task<? super T, R> task, Function<List<R>, List<R>> after) {
        return withParallel(Function.identity(), task, after);
    }

    /*
     * 并行任务（模板方法：算法骨架已然固定）
     * 通过方法重载提供默认钩子 Function.identity()
     */
    static <T, R> Task<List<T>, List<R>> withParallel(Function<List<T>, List<T>> before, Task<? super T, R> task, Function<List<R>, List<R>> after) {
        Assert.isTrue(before, Predicates::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        Assert.isTrue(task, Predicates::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        Assert.isTrue(after, Predicates::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        final var atomicReference = new AtomicReference<CompletableFuture<R>[]>();
        return list -> CompletableFuture.completedFuture(list)
                .thenApplyAsync(before, taskExecutor()) // 参数前置处理
                .thenApplyAsync(ts -> atomicReference.updateAndGet(_ -> ts.stream().map(task).toArray(CompletableFuture[]::new)), taskExecutor()) // 并行执行任务
                .thenComposeAsync(CompletableFuture::allOf, taskExecutor()) // 等待所有任务完成
                .thenApplyAsync(_ -> Arrays.stream(atomicReference.get()).map(CompletableFuture<R>::join).toList(), taskExecutor()) // 汇总任务结果
                .thenApplyAsync(after, taskExecutor()); // 返回值后置处理
    }

    /**
     * 顺序任务
     *
     * <pre>
     * {@code // 内含阻塞原语，会退化至同步
     *      return ts -> CompletableFuture.completedFuture(ts)
     *              .thenApplyAsync(tList -> tList.stream()
     *                  .map(task)
     *                  .map(CompletableFuture::join) // 退化至同步
     *                  .toList(), taskExecutor());
     * }
     *
     * {@code // 写法略丑，不够优雅
     * return ts -> ts.stream().reduce(CompletableFuture.completedFuture(new ArrayList<R>()),
     *                 (future, t) -> future.thenComposeAsync(rs -> task.apply(t).thenAcceptAsync(rs::add, taskExecutor()).thenApplyAsync(_ -> rs, taskExecutor()), taskExecutor()),
     *                 (left, right) -> {
     *                     throw new UnsupportedOperationException("not here");
     *                 });
     * }
     * </pre>
     */
    static <T, R> Task<List<T>, List<R>> withSequential(Task<? super T, R> task) {
        Assert.isTrue(task, Predicates::isNotNull, () -> new NullPointerException("To strive, to seek, to find, and not to yield. — Alfred Lord Tennyson"));
        return ts -> {
            final var rs = new ArrayList<R>();
            return CompletableFuture.completedFuture(ts)
                    .thenApplyAsync(List::iterator, taskExecutor())
                    .thenComposeAsync(iterator -> Task.<T, R, List<R>>withIteratorAsync(task, rs).apply(iterator), taskExecutor())
                    .thenApplyAsync(_ -> rs, taskExecutor());
        };
    }

    private static <T, R, L extends List<? super R>> Task<Iterator<T>, Iterator<T>> withIteratorAsync(Task<? super T, R> task, L rs) {
        Assert.isTrue(task, Predicates::isNotNull, () -> new NullPointerException("To strive, to seek, to find, and not to yield. — Alfred Lord Tennyson"));
        Assert.isTrue(rs, Predicates::isNotNull, () -> new NullPointerException("To strive, to seek, to find, and not to yield. — Alfred Lord Tennyson"));
        return iterator -> iterator.hasNext() ? task.execute(iterator.next())
                .thenAcceptAsync(rs::add, taskExecutor())
                .thenComposeAsync(_ -> Task.<T, R, L>withIteratorAsync(task, rs).apply(iterator), taskExecutor())
                : CompletableFuture.completedFuture(iterator);
    }

    /*
     * 自迭代任务
     */
    static <T> Task<T, T> withRepeatUntil(Task<? super T, T> task, Predicate<T> until) {
        Assert.isTrue(task, Predicates::isNotNull, () -> new NullPointerException("Your time is limited, so don’t waste it living someone else’s life. — Steve Jobs"));
        Assert.isTrue(until, Predicates::isNotNull, () -> new NullPointerException("Your time is limited, so don’t waste it living someone else’s life. — Steve Jobs"));
        return t -> until.test(t) ? CompletableFuture.completedFuture(t) : task.execute(t).thenComposeAsync(withRepeatUntil(task, until), taskExecutor());
    }

    /*
     * 流控任务专员（装饰器模式）
     */
    static <T, R> Task<T, ? extends R> withRateLimit(Task<? super T, R> innerTask, long delay) {
        Assert.isTrue(innerTask, Predicates::isNotNull, () -> new NullPointerException("The only way to do great work is to love what you do. — Steve Jobs"));
        Assert.isTrue(delay, 0L, Predicates::isOrderGt, () -> new IllegalArgumentException("The only way to do great work is to love what you do. — Steve Jobs"));
        return t -> CompletableFuture.completedFuture(t)
                .thenApplyAsync(RateLimiter::acquire, taskExecutor()) // 执行任务前获取信号量
                .thenComposeAsync(innerTask, delayedTaskExecutor(delay)) // 使用包装后带延时的线程池
                .whenCompleteAsync(RateLimiter::release, taskExecutor()); // 任务结束时释放信号量
    }

    /*
     * 任务专用线程池（使用经过包装的虚拟线程池，从当前上下文中获取 ScopedValue 并绑定至新开启的虚拟线程）
     */
    static ScopedExecutor taskExecutor() {
        return ScopedExecutor.ScopedExecutors.newScopedExecutor();
    }

    /*
     * 带延时的任务专用线程池
     */
    static Executor delayedTaskExecutor(long delay) {
        return CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS, taskExecutor());
    }
}