package com.tterrag.chatmux.api.bridge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatSource<M extends ChatMessage<M>> {

    ChatService<M> getType();

    Flux<M> connect(String channel);

    Mono<M> send(String channel, ChatMessage<?> payload, boolean raw);

    void disconnect(String channel);
}
