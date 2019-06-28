package com.tterrag.chatmux.factorio;

import java.io.File;

import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@Slf4j
public class FactorioSource implements ChatSource {
    
    private static final String GLOBAL_CHAT = "/silent-command game.print(\"%1$s\")";
    private static final String TEAM_CHAT = "/silent-command game.forces[\"%2$s\"].print(\"%1$s\")";

    private boolean connected;

    @NonNull
    private final FactorioClient factorio = new FactorioClient(new File(FactorioService.getInstance().getData().getInput()), new File(FactorioService.getInstance().getData().getOutput()));
    
    @Override
    public ChatService getType() {
        return FactorioService.getInstance();
    }
    
    @Override
    public Flux<FactorioMessage> connect(String channel) {
        if (!connected) {
            factorio.connect().subscribe();
            connected = true;
        }
        return factorio.inbound()
                .filter(m -> FactorioClient.GLOBAL_TEAM.equals(m.getChannel()) || m.getChannel().equals(channel));
    }

    @Override
    public Mono<Void> send(String channel, ChatMessage message, boolean raw) {
        String content = raw ? message.getContent() : message.toString();
        return Mono.just(factorio.outbound())
                .doOnNext(sink -> sink.next(String.format(FactorioClient.GLOBAL_TEAM.equals(channel) ? GLOBAL_CHAT : TEAM_CHAT, content.replaceAll("\\r?\\n", "\\\\n"), channel)))
                .then();
    }

    @Override
    public void disconnect(String channel) {}
}