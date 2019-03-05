package com.tterrag.chatmux.bridge;

import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Flux;

public interface ChatSource<I, O> {
    
    public ChatService<I, O> getType();
    
    public Flux<? extends ChatMessage> connect(WebSocketClient<I, O> client, String channel);
}
