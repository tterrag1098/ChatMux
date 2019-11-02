package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Flux;

public interface ChatChannel<M extends ChatMessage<M>> {
    
    String getName();
    
    ChatService<M> getService();
    
    Flux<M> connect();

}
