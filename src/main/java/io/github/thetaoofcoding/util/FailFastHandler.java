package io.github.thetaoofcoding.util;

import lombok.extern.slf4j.Slf4j;
import io.github.thetaoofcoding.flow.FlowEngine;

@Slf4j
public class FailFastHandler {
    public static <T> T handle(Throwable t) {
        log.error("\u001B[91m敕令：「心念不纯，符窍无光！僭请神明，触怒天罡！伏请三清垂慈，赦宥愚诚！」\u001B[0m");
        FlowEngine.HTTP_CLIENT.close();
        ScopedExecutor.shutdown();
        t.printStackTrace();
        System.exit(1);
        return null;
    }
}