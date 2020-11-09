package com.tterrag.chatmux.api.link;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatService;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface Link {
    
    ChatChannel<?> getFrom();
    
    ChatChannel<?> getTo();
    
    boolean isRaw();
    
    Disposable getSubscription();
    
    Mono<String> prettyPrint(ChatService<?> target);

}
