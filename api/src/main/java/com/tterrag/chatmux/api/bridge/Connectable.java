package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Flux;

public interface Connectable {
    
    Flux<? extends ChatMessage<?>> connect(String input);
    
    default <R extends ChatMessage<R>> Flux<R> connect(ChatChannel<R> channel) {
        return connect(channel.getService(), channel.getName());
    }
    
    default <R extends ChatMessage<R>> Flux<R> connect(ChatService<R> service, String channel) {
        return service.getSource().connect(channel);
    }
}
