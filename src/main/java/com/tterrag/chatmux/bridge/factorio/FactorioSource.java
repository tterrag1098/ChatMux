package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.links.ChatSource;
import com.tterrag.chatmux.util.Service;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Flux;

public class FactorioSource implements ChatSource<FactorioMessage, String> {
    
    @Override
    public Service<FactorioMessage, String> getType() {
        return Service.FACTORIO;
    }
    
    @Override
    public Flux<FactorioMessage> connect(WebSocketClient<FactorioMessage, String> client, String channel) {
        return client.inbound()
                .filter(m -> FactorioClient.GLOBAL_TEAM.equals(channel) || m.getChannel().equals(channel));
    }
}