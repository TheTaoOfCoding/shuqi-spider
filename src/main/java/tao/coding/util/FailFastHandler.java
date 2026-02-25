package tao.coding.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FailFastHandler {
    public static <T> T handle(Throwable t) {
        t.printStackTrace();
        log.error("\u001B[91m敕令：「心念不纯，符窍无光！僭请神明，触怒天罡！伏请三清垂慈，赦宥愚诚！」\u001B[0m");
        System.exit(1);
        return null;
    }
}