package com.tterrag.chatmux.api.wiretap;

import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import com.tterrag.chatmux.api.bridge.ChatMessage;

import reactor.core.publisher.Mono;

@Extension
public interface WiretapPlugin extends ExtensionPoint {
    
    Mono<Void> onMessage(ChatMessage<?> msg);

}
