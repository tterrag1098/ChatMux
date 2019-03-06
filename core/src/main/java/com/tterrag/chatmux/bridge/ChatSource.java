package com.tterrag.chatmux.bridge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatSource<I, O> {
    
    public ChatService<I, O> getType();

    /**
     * Parse raw user input into a "real" channel name. For instance, converting from discord mention ({@code <#\d+>})
     * to just the snowflake ID.
     */
    default Mono<String> parseChannel(String channel) {
        return Mono.just(channel);
    }

    public Flux<? extends ChatMessage> connect(String channel);
    
    public Mono<Void> send(String channel, ChatMessage payload, boolean raw);
            
    public void disconnect(String channel);
}
