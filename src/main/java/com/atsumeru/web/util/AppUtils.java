package com.atsumeru.web.util;

import java.util.function.Supplier;

public class AppUtils {

    public static void sleepThread(int millis) {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sleepWhile(int millis, Supplier<Boolean> supplier) {
        do {
            sleepThread(millis);
        } while (supplier.get());
    }
}
