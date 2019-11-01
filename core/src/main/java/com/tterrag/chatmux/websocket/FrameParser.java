package com.tterrag.chatmux.websocket;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.UnicastProcessor;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyPipeline;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.util.annotation.NonNull;

/**
 * Parts of this class adapted from <a href="https://github.com/Discord4J/Discord4J">Discord4J</a>, licensed under
 * LGPLv3.
 */
@Slf4j
@RequiredArgsConstructor
public class FrameParser<I, O> implements ConnectionObserver {

    private static class CloseHandlerAdapter extends ChannelInboundHandlerAdapter {

        private final AtomicReference<CloseStatus> closeStatus;

        private CloseHandlerAdapter(AtomicReference<CloseStatus> closeStatus) {
            this.closeStatus = closeStatus;
        }

        @SuppressWarnings("null")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof CloseWebSocketFrame && ((CloseWebSocketFrame) msg).isFinalFragment()) {
                CloseWebSocketFrame close = (CloseWebSocketFrame) msg;
                log.debug("Close status: {} {}", close.statusCode(), close.reasonText());
                closeStatus.set(new CloseStatus(close.statusCode(), close.reasonText()));
            }
            ctx.fireChannelRead(msg);
        }

        @SuppressWarnings("null")
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof SslCloseCompletionEvent) {
                SslCloseCompletionEvent closeEvent = (SslCloseCompletionEvent) evt;
                if (!closeEvent.isSuccess()) {
                    log.debug("Abnormal close status: {}", closeEvent.cause().toString());
                    closeStatus.set(new CloseStatus(1006, closeEvent.cause().toString()));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
    
    private final Function<String, I> deserializer;
    private final Function<O, String> serializer;

    @NonNull
    private final UnicastProcessor<I> inboundExchange = UnicastProcessor.create();
    @NonNull
    private final UnicastProcessor<O> outboundExchange = UnicastProcessor.create();
    private final MonoProcessor<Void> completionNotifier = MonoProcessor.create();

    public FrameParser(ObjectMapper mapper, Class<? extends I> inputType) {
        this(s -> {
            try {
                return mapper.readValue(s, inputType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, mapper);
    }
    
    public FrameParser(Function<String, I> deserializer, ObjectMapper mapper) {
        this(deserializer, t -> {
            try {
                return mapper.writeValueAsString(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public void onStateChange(@NonNull Connection connection, @NonNull State newState) {
        log.debug("{} {}", newState, connection);
    }

    public Mono<Void> handle(WebsocketInbound in, WebsocketOutbound out) {
        AtomicReference<CloseStatus> reason = new AtomicReference<>();
        in.withConnection(connection -> connection.addHandlerLast("client.last.closeHandler", new CloseHandlerAdapter(reason)));
    
        Mono<Void> outSub = out.options(NettyPipeline.SendOptions::flushOnEach)
            .sendObject(outboundExchange.log(log.getName()).map(serializer::apply).map(TextWebSocketFrame::new))
            .then()
            .log(log.getName() + ".out")
            .doOnError(t -> log.debug("Sender encountered an error", t))
            .doOnSuccess(v -> log.debug("Sender succeeded"))
            .doOnCancel(() -> log.debug("Sender cancelled"))
            .doOnTerminate(() -> log.debug("Sender terminated"));
    
        Mono<Void> inSub = in.receiveFrames()
            .map(WebSocketFrame::content)
            .map(buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new String(bytes);
            })
            .map(deserializer::apply)
            .doOnNext(inboundExchange::onNext)
            .doOnError(t -> log.error("Exception receiving frame", t))
            .doOnComplete(() -> log.debug("Receiver completed"))
            .doOnCancel(() -> log.debug("Receiver canceled"))
            .doOnTerminate(() -> log.debug("Receiver terminated"))
            .doOnComplete(() -> {
                log.debug("Receiver completed");
                CloseStatus closeStatus = reason.get();
                if (closeStatus != null) {
                    log.debug("Forwarding close reason: {}", closeStatus);
                    log.debug("Triggering error sequence ({})", new CloseException(closeStatus).toString());
                    outboundExchange.onNext(null);
                    log.debug("Preparing to complete outbound exchange after error");
                    outboundExchange.onComplete();                             }
            })
            .then()
            .log(log.getName() + ".in");
        
        return Mono.when(outSub, inSub);
    }

    /**
     * Initiates a close sequence with the given error. It will terminate this session with an error signal on the
     * {@link #handle(reactor.netty.http.websocket.WebsocketInbound, reactor.netty.http.websocket.WebsocketOutbound)}
     * method, while completing both exchanges through normal complete signals.
     * <p>
     * The error can then be channeled downstream and acted upon accordingly.
     *
     * @param error the cause for this session termination
     */
    public void error(Throwable error) {
        log.debug("Triggering error sequence ({})", error.toString());
        if (!completionNotifier.isTerminated()) {
            if (error instanceof CloseException) {
                log.debug("Signaling completion notifier as error with same CloseException");
                completionNotifier.onError(error);
            } else {
                log.debug("Signaling completion notifier as error with wrapping CloseException");
                completionNotifier.onError(new CloseException(new CloseStatus(1006, error.toString()), error));
            }
        }
        outboundExchange.onNext(null);
        log.debug("Preparing to complete outbound exchange after error");
        outboundExchange.onComplete();
        log.debug("Preparing to complete inbound exchange after error");
        inboundExchange.onComplete();
    }
    
    public void close() {
        log.debug("Triggering close sequence - signaling completion notifier");
        completionNotifier.onComplete();
        log.debug("Preparing to complete outbound exchange after close");
        outboundExchange.onComplete();
        log.debug("Preparing to complete inbound exchange after close");
        inboundExchange.onComplete();
    }

    public UnicastProcessor<I> inbound() {
        return inboundExchange;
    }
    
    public UnicastProcessor<O> outbound() {
        return outboundExchange;
    }
}
