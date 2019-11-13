package com.tterrag.chatmux.api.command;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommandContext<M extends ChatMessage<M>> {

    ChatService<M> getService();

    String getArgs();

    default String[] getSplitArgs() {
        return getSplitArgs("\\s+");
    }

    default String[] getSplitArgs(String split) {
        return getArgs().split(split);
    }

    /**
     * The channel this command was sent from, in a form acceptable to create new links with.
     * 
     * @return The channel ID, which may be used to create new links
     */
    String getChannelId();

    /**
     * Get an identifier for the user who sent this command, which is unique to the service given by
     * {@link #getService()}.
     * 
     * @return The unique identifier for the executing user
     */
    String getUserId();

    Mono<M> reply(String msg);
    
    ChatService<?> getService(String name);
    
    Flux<? extends ChatMessage<?>> connect(String input);
    
    default <R extends ChatMessage<R>> Flux<R> connect(ChatChannel<R> channel) {
        return connect(channel.getService(), channel.getName());
    }
    
    default <R extends ChatMessage<R>> Flux<R> connect(ChatService<R> service, String channel) {
        return service.getSource().connect(channel);
    }
    
    default Flux<M> connect() {
        return connect(getService(), getChannelId());
    }
}
