package com.tterrag.chatmux.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
@Slf4j
public class SimpleWebSocketClient<I, O> implements WebSocketClient<I, O> {
    
    private final EmitterProcessor<I> receiver = EmitterProcessor.create(false);
    private final EmitterProcessor<O> sender = EmitterProcessor.create(false);

    // initialize the sinks to safely produce values downstream
    // we use LATEST backpressure handling to avoid overflow on no subscriber situations
    private final FluxSink<I> receiverSink = receiver.sink(FluxSink.OverflowStrategy.LATEST);
    private final FluxSink<O> senderSink = sender.sink(FluxSink.OverflowStrategy.LATEST); 
        
    public Mono<Void> connect(@NonNull String url, FrameParser<I, O> handler) {
        return Mono.defer(() -> {
            // Subscribe each inbound GatewayPayload to the receiver sink
            Disposable inboundSub = handler.inbound()
                    .doOnError(t -> log.debug("Inbound encountered an error", t))
                    .doOnCancel(() -> log.debug("Inbound cancelled"))
                    .doOnComplete(() -> log.debug("Inbound completed"))
                    .subscribe(receiverSink::next);

            // Subscribe the receiver to process and transform the inbound payloads into Dispatch events
            Disposable receiverSub = receiver.log().doOnError(Throwable::printStackTrace).subscribe();

            // Subscribe the handler's outbound exchange with our outgoing signals
            // routing error and completion signals to close the gateway
            Disposable senderSub = sender.log().subscribe(handler.outbound()::onNext, t -> { t.printStackTrace(); handler.close(); }, handler::close);

            return HttpClient.create()
                    .observe((connection, newState) -> log.debug("{} {}", newState, connection))
                    .wiretap(true)
                    .websocket()
                    .uri(url)
                    .handle(handler::handle)
                    .doOnError(Throwable::printStackTrace)
                    .doOnTerminate(() -> {
                        log.debug("Terminating websocket client, disposing subscriptions");
                        inboundSub.dispose();
                        receiverSub.dispose();
                        senderSub.dispose();
                    })
                    .then();
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
