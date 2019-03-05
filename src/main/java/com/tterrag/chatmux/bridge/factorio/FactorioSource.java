package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Flux;

public class FactorioSource implements ChatSource<FactorioMessage, String> {
    
    @Override
    public ChatService<FactorioMessage, String> getType() {
        return ChatService.FACTORIO;
    }
    
    @Override
    public Flux<FactorioMessage> connect(WebSocketClient<FactorioMessage, String> client, String channel) {
        return client.inbound()
                .filter(m -> FactorioClient.GLOBAL_TEAM.equals(channel) || m.getChannel().equals(channel));
    }
}