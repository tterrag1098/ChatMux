package com.tterrag.chatmux.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleServiceConfig<T extends ServiceData> implements ServiceConfig<T> {
    
    private final Supplier<T> creator;
    private final Consumer<T> callback;

    @Override
    public T makeDefault() {
        return creator.get();
    }
    
    @Override
    public void onLoad(T data) {
        callback.accept(data);
    }
}
