package com.tterrag.chatmux.config;


public interface ServiceConfig<T extends ServiceData> {
    
    T makeDefault();

    void onLoad(T data);
    
}
