package com.tterrag.chatmux.websocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public interface WebSocketClient<I, O> {
    
    Flux<I> inbound();
    
    FluxSink<O> outbound();

    Mono<Void> connect(@NonNull String string, FrameParser<I, O> frameParser);

}
