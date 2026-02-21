package tao.coding.util;

import java.util.concurrent.Semaphore;

public class RateLimiter {
    // 用于控制下载章节内容时的休眠时间 : 别改！别改！别改！后果自负！！！
    public static final long DELAY = 2L;
    // 在下载章节内容时，最大允许并发数
    public static final Integer MAX_ALLOWED = 3;
    // 信号量
    public static final Semaphore SEMAPHORE = new Semaphore(MAX_ALLOWED);

    // 获取信号量
    public static <T> T acquire(T t) {
        try {
            SEMAPHORE.acquire();
            return t;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 释放信号量
    public static <R, E extends Throwable> void release(R r, E e) {
        SEMAPHORE.release();
    }
}
