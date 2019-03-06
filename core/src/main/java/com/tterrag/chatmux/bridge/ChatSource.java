package com.tterrag.chatmux.bridge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatSource<I, O> {
    
    public ChatService<I, O> getType();
    
    public Flux<? extends ChatMessage> connect(String channel);
    
    public Mono<Void> send(String channel, ChatMessage payload, boolean raw);
            
    public void disconnect(String channel);
}
