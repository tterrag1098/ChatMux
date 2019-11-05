package com.tterrag.chatmux.api.wiretap;

import org.pf4j.ExtensionPoint;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;

import reactor.core.publisher.Mono;

public interface WiretapPlugin extends ExtensionPoint {
    
    <M extends ChatMessage<M>> Mono<Void> onMessage(M msg, ChatChannel<M> channel);

}
