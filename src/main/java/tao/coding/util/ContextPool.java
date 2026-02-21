package tao.coding.util;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JS 引擎池：缓存JS引擎对象
 */
@Slf4j
public class ContextPool {
    // JS 脚本
    private static final String JS_SCRIPT;
    // 阻塞队列
    private static final BlockingDeque<Context> BLOCKING_DEQUE;
    // 組件名稱
    private static final String NAME = "「三清铃」";

    static {
        // 禁用部分警告
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        log.info("{} - 执行初始化js引擎池", NAME);
        try (var resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream("decode.js")) {
            Assert.isTrue(resourceStream, Assert::isNotNull, () -> new NullPointerException("To be, or not to be, that is the question. — William Shakespeare, Hamlet"));
            JS_SCRIPT = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
            // 缓存300个 Context
            BLOCKING_DEQUE = IntStream.rangeClosed(1, 300)
                    .mapToObj(_ -> createContext())
                    .collect(Collectors.toCollection(LinkedBlockingDeque::new));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 创建 Context
     */
    private static Context createContext() {
        try {
            var context = Context.newBuilder("js")
                    .allowAllAccess(true)
                    .build();
            context.eval("js", JS_SCRIPT);
            return context;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 获取 Context
     */
    public static Context acquire() {
        try {
            return BLOCKING_DEQUE.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 释放 Context
     */
    public static void release(Context context) {
        try {
            BLOCKING_DEQUE.put(context);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}