package com.tterrag.chatmux.twitch;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatSource;
import com.tterrag.chatmux.api.websocket.WebSocketClient;
import com.tterrag.chatmux.twitch.irc.IRCEvent;
import com.tterrag.chatmux.websocket.SimpleFrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
@Slf4j
public class TwitchSource implements ChatSource<TwitchMessage> {
    
    private final TwitchRequestHelper helper;
    private boolean connected;

    @NonNull
    private final WebSocketClient<IRCEvent, String> send = new SimpleWebSocketClient<>();
    @NonNull
    private final WebSocketClient<IRCEvent, String> receive = new SimpleWebSocketClient<>();
    
    private final Set<String> sentMessages = Sets.newConcurrentHashSet();
    
    @Override
    public TwitchService getType() {
        return TwitchService.getInstance();
    }
    
    private volatile Flux<TwitchMessage> messageRelay;
    
    @Override
    public Flux<TwitchMessage> connect(String channel) {
        if (!connected) {
            send.connect("wss://irc-ws.chat.twitch.tv:443", new SimpleFrameParser<>(IRCEvent::parse, Function.identity()))
                .subscribe($ -> {}, t -> log.error("Twitch websocket completed with error", t), () -> log.error("Twitch websocket completed"));
            
            send.outbound()
                .next("PASS oauth:" + TwitchService.getInstance().getData().getTokenSend())
                .next("NICK " + TwitchService.getInstance().getData().getNickSend())
                .next("CAP REQ :twitch.tv/tags")
                .next("CAP REQ :twitch.tv/commands");
            
            receive.connect("wss://irc-ws.chat.twitch.tv:443", new SimpleFrameParser<>(IRCEvent::parse, Function.identity()))
                .subscribe($ -> {}, t -> log.error("Twitch websocket completed with error", t), () -> log.error("Twitch websocket completed"));
        
            receive.outbound()
                .next("PASS oauth:" + TwitchService.getInstance().getData().getTokenReceive())
                .next("NICK " + TwitchService.getInstance().getData().getNickReceive())
                .next("CAP REQ :twitch.tv/tags")
                .next("CAP REQ :twitch.tv/commands");
            connected = true;
        }
        final String lcChan = channel.toLowerCase(Locale.ROOT);
        send.outbound().next("JOIN #" + lcChan);
        receive.outbound().next("JOIN #" + lcChan);
        
        Flux<IRCEvent.Ping> pingPongSend = send.inbound().ofType(IRCEvent.Ping.class)
                .doOnNext(p -> send.outbound().next("PONG :tmi.twitch.tv"));
        Flux<IRCEvent.Ping> pingPongReceive = receive.inbound().ofType(IRCEvent.Ping.class)
                .doOnNext(p -> receive.outbound().next("PONG :tmi.twitch.tv"));
        
        synchronized (this) {
            if (messageRelay == null) {
                messageRelay = receive.inbound().ofType(IRCEvent.Message.class)
                        .filter(e -> !sentMessages.remove(e.getContent()))
                        .flatMap(e -> helper.getUser(e.getUser())
                                            .zipWith(helper.getUser(e.getChannel()),
                                                    (u, c) -> new TwitchMessage(receive, e, c.displayName, u.displayName, u.avatarUrl)))
                        .doOnTerminate(() -> { synchronized(TwitchSource.this) { messageRelay = null; }})
                        .share();
            }
            return Flux.merge(pingPongSend, pingPongReceive, messageRelay)
                    .ofType(TwitchMessage.class)
                    .filter(e -> e.getChannel().equals(lcChan));
        }
    }
    
    @Override
    public Mono<TwitchMessage> send(String channel, ChatMessage<?> message, boolean raw) {
        String content = raw ? message.getContent() : message.toString();
        String username = TwitchService.getInstance().getData().getNickSend();
        return Mono.just(send.outbound())
                .doOnNext($ -> sentMessages.add(content))
                .doOnNext(sink -> sink.next("PRIVMSG #" + channel.toLowerCase(Locale.ROOT) + " :" + content))
                .thenReturn(new TwitchMessage(send, new IRCEvent.Message(ImmutableMap.of(), username, channel, content), channel, username, null)); // TODO have a second websocket reading our own message events
    }

    @Override
    public void disconnect(String channel) {
        send.outbound().next("PART #" + channel);
        receive.outbound().next("PART #" + channel);
    }
}