package com.tterrag.chatmux.links;

import com.tterrag.chatmux.util.Service;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Flux;

public interface ChatSource<I, O> {
    
    public Service<I, O> getType();
    
    public Flux<? extends Message> connect(WebSocketClient<I, O> client, String channel);
}
