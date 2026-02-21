package tao.coding.util;

import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * CompletableFuture 专用虚拟线程池工具类，为每一个新开启的虚拟线程设置 ScopedValue 值
 */
@SuppressWarnings("unused")
public interface ScopedExecutor extends Executor {

    static ExecutorService delegate() {
        return ScopedExecutors.DELEGATE;
    }

    static void shutdown() {
        delegate().shutdown();
    }

    class ScopedExecutors {
        // IO密集型任务线程池：使用虚拟线程池 （静态代理对象）
        private static final ExecutorService DELEGATE = Executors.newVirtualThreadPerTaskExecutor();
        // 线程本地变量：传递当前下载的书籍名称
        public static final ScopedValue<String> KEY = ScopedValue.newInstance();

        public static ScopedExecutor newScopedExecutor() {
            // 在提交任务的线程中获取值
            return newScopedExecutor(KEY, KEY.get());
        }

        public static ScopedExecutor newScopedExecutor(String value) {
            return newScopedExecutor(KEY, value);
        }

        public static ScopedExecutor newScopedExecutor(Supplier<String> value) {
            return newScopedExecutor(KEY, value);
        }

        public static <T> ScopedExecutor newScopedExecutor(ScopedValue<T> key, Supplier<T> value) {
            return newScopedExecutor(key, value.get());
        }

        // 装饰器 + 静态代理
        public static <T> ScopedExecutor newScopedExecutor(ScopedValue<T> key, T value) {
            // 在新创建的虚拟线程中重新绑定值
            return r -> DELEGATE.execute(() -> ScopedValue.where(key, value).run(r));
        }

        // 动态代理 + 静态代理
        public static <T> ScopedExecutor proxy(ScopedValue<T> key) {
            return (ScopedExecutor) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{ScopedExecutor.class}, (_, method, args) -> {
                if ("execute".equals(method.getName()) && args.length > 0 && args[0] instanceof Runnable r) {
                    // 在提交任务的线程中获取值
                    var value = key.get();
                    // 在新创建的虚拟线程中重新绑定值
                    args[0] = (Runnable) () -> ScopedValue.where(key, value).run(r);
                }
                return method.invoke(DELEGATE, args);
            });
        }
    }
}