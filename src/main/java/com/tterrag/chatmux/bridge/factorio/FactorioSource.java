package com.tterrag.chatmux.bridge.factorio;

import java.io.File;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.websocket.FactorioClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public class FactorioSource implements ChatSource<FactorioMessage, String> {

    private boolean connected;

    @NonNull
    private final FactorioClient factorio = new FactorioClient(new File(Main.cfg.getFactorio().getInput()), new File(Main.cfg.getFactorio().getOutput()));
    
    @Override
    public ChatService<FactorioMessage, String> getType() {
        return ChatService.FACTORIO;
    }
    
    @Override
    public Flux<FactorioMessage> connect(String channel) {
        return raw(channel);
    }

    @Override
    public Flux<FactorioMessage> raw(String channel) {
        if (!connected) {
            factorio.connect().subscribe();
            connected = true;
        }
        return factorio.inbound()
                .filter(m -> FactorioClient.GLOBAL_TEAM.equals(channel) || m.getChannel().equals(channel));
    }

    @Override
    public Mono<Void> send(String channel, ChatMessage message, boolean raw) {
        return Mono.just(factorio.outbound())
                .doOnNext(sink -> sink.next(raw ? message.getContent() : message.toString()))
                .then();
    }

    @Override
    public void disconnect(String channel) {}
}