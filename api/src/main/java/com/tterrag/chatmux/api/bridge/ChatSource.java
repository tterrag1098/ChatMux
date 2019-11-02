package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatSource<M extends ChatMessage<M>> {
    
    ChatService<M> getType();

    /**
     * Parse raw user input into a "real" channel name. For instance, converting from discord mention ({@code <#\d+>})
     * to just the snowflake ID.
     */
    default Mono<String> parseChannel(String channel) {
        return Mono.just(channel);
    }

    Flux<M> connect(String channel);
    
    Mono<Void> send(String channel, ChatMessage<?> payload, boolean raw);
            
    void disconnect(String channel);
}
