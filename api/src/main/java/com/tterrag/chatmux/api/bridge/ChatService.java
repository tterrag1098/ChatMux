package com.tterrag.chatmux.api.bridge;

import com.tterrag.chatmux.api.config.ServiceConfig;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public interface ChatService<M extends ChatMessage<M>> {

    String getName();
    
    void initialize();
    
    ChatSource<M> getSource();
    
    @Nullable ServiceConfig<?> getConfig();

    /**
     * Parse raw user input into a "real" channel name. For instance, converting from discord mention ({@code <#\d+>})
     * to just the snowflake ID.
     */
    default Mono<String> parseChannel(String channel) {
        return Mono.just(channel);
    }

    /**
     * The reverse of {@link #parseChannel(String)}, turn "real" channel name into a readable one for user interfaces.
     * 
     * @param channel
     *            The internal channel ID
     * @return The pretty channel name for UIs
     */
    default Mono<String> prettifyChannel(String channel) {
        return Mono.just(channel);
    }

}
