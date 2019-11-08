package com.tterrag.chatmux.api.websocket;

import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public interface IFrameParser<I, O> extends ConnectionObserver {

    Mono<Void> handle(WebsocketInbound in, WebsocketOutbound out);

    UnicastProcessor<I> inbound();

    UnicastProcessor<O> outbound();

    void close();

}
