package com.tterrag.chatmux.api.bridge;

import com.tterrag.chatmux.api.config.ServiceConfig;

import reactor.util.annotation.Nullable;

public interface ChatService<M extends ChatMessage<M>> {

    String getName();
    
    void initialize();
    
    ChatSource<M> getSource();
    
    @Nullable ServiceConfig<?> getConfig();
}
