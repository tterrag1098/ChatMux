package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Mono;

public interface ChatMessage<M extends ChatMessage<M>> {

    ChatService<M> getService();
    
    String getChannel();
    
    String getChannelId();
    
    String getUser();
    
    String getContent();
    
    String getAvatar();
    
    Mono<Void> delete();
    
    Mono<Void> kick();
    
    Mono<Void> ban();
}
