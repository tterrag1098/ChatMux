package com.tterrag.chatmux.api.link;

import com.tterrag.chatmux.api.bridge.ChatChannel;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface Link {
    
    ChatChannel<?> getFrom();
    
    ChatChannel<?> getTo();
    
    boolean isRaw();
    
    Disposable getSubscription();
    
    Mono<String> prettyPrint();

}
