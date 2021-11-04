package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

public interface ChatChannel<M extends ChatMessage<M>> {
    
    String getName();
    
    @Nullable
    ChatService<M> getService();
    
    static <M extends ChatMessage<M>> Flux<M> connect(ChatChannel<M> channel) {
        return channel.getService().getSource().connect(channel.getName());
    }
}
