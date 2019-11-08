package com.tterrag.chatmux.websocket;

import com.tterrag.chatmux.api.websocket.IFrameParser;
import com.tterrag.chatmux.api.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.annotation.NonNull;

/**
 * Parts of this class adapted from <a href="https://github.com/Discord4J/Discord4J">Discord4J</a>, licensed under
 * LGPLv3.
 */
@RequiredArgsConstructor
@Slf4j
public class SimpleWebSocketClient<I, O> implements WebSocketClient<I, O> {
    
    @NonNull
    private final EmitterProcessor<I> receiver = EmitterProcessor.create(false);
    @NonNull
    private final EmitterProcessor<O> sender = EmitterProcessor.create(false);

    // initialize the sinks to safely produce values downstream
    // we use LATEST backpressure handling to avoid overflow on no subscriber situations
    @NonNull
    private final FluxSink<I> receiverSink = receiver.sink(FluxSink.OverflowStrategy.LATEST);
    @NonNull
    private final FluxSink<O> senderSink = sender.sink(FluxSink.OverflowStrategy.LATEST);
        
    public Mono<Void> connect(String url, IFrameParser<I, O> handler) {
        return Mono.defer(() -> {
            // Subscribe each inbound GatewayPayload to the receiver sink
            Flux<I> inboundSub = handler.inbound()
                    .doOnError(t -> log.debug("Inbound encountered an error", t))
                    .doOnCancel(() -> log.debug("Inbound cancelled"))
                    .doOnComplete(() -> log.debug("Inbound completed"))
                    .doOnNext(receiverSink::next);

            // Subscribe the receiver to process and transform the inbound payloads into Dispatch events
            Flux<I> receiverSub = receiver.log(log.getName()).doOnError(t -> log.error("Exception receiving websocket data", t));

            // Subscribe the handler's outbound exchange with our outgoing signals
            // routing error and completion signals to close the gateway
            Flux<O> senderSub = sender.log(log.getName())
                    .doOnNext(handler.outbound()::onNext)
                    .doOnError(t -> handler.close())
                    .doOnComplete(handler::close);

            Mono<Void> ws = HttpClient.create()
                    .observe((connection, newState) -> log.debug("{} {}", newState, connection))
                    .wiretap(true)
                    .websocket()
                    .uri(url)
                    .handle(handler::handle)
                    .doOnError(t -> log.error("Exception handling websocket data", t))
                    .doOnTerminate(() -> {
                        log.debug("Terminating websocket client, disposing subscriptions");
                    })
                    .then();
            
            return Mono.when(inboundSub, receiverSub, senderSub, ws);
        });
    }

    @Override
    public Flux<I> inbound() {
        return receiver;
    }
    
    @Override
    public FluxSink<O> outbound() {
        return senderSink;
    }
}
