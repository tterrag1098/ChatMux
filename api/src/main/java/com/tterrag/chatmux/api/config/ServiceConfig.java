package com.tterrag.chatmux.api.config;

public interface ServiceConfig<T extends ServiceData> {
    
    T makeDefault();

    void onLoad(T data);
    
}
