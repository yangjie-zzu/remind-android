package com.freefjay.remind.utils;

import java.util.function.Consumer;

public class BeanUtil {

    @FunctionalInterface
    public static interface GetFunc<T> {
        T get();
    }

    public static <T> T create(GetFunc<T> getFunc, Consumer<T> consumer) {
        T t = getFunc.get();
        consumer.accept(t);
        return t;
    }

}
