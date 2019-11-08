package com.tterrag.chatmux.api.websocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public interface WebSocketClient<I, O> {
    
    Flux<I> inbound();
    
    FluxSink<O> outbound();

    Mono<Void> connect(String string, IFrameParser<I, O> frameParser);

}
