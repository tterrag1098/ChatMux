package com.tterrag.chatmux.twitch;

import java.util.Locale;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatSource;
import com.tterrag.chatmux.twitch.irc.IRCEvent;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

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
    private final WebSocketClient<IRCEvent, String> twitch = new SimpleWebSocketClient<>();
    
    @Override
    public TwitchService getType() {
        return TwitchService.getInstance();
    }
    
    @Override
    public Flux<TwitchMessage> connect(String channel) {
        if (!connected) {
            twitch.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
                .subscribe($ -> {}, t -> log.error("Twitch websocket completed with error", t), () -> log.error("Twitch websocket completed"));
            
            twitch.outbound()
                .next("PASS oauth:" + TwitchService.getInstance().getData().getToken())
                .next("NICK " + TwitchService.getInstance().getData().getNick())
                .next("CAP REQ :twitch.tv/tags")
                .next("CAP REQ :twitch.tv/commands");
            connected = true;
        }
        final String lcChan = channel.toLowerCase(Locale.ROOT);
        twitch.outbound().next("JOIN #" + lcChan);
        
        Flux<IRCEvent.Ping> pingPong = twitch.inbound().ofType(IRCEvent.Ping.class)
                .doOnNext(p -> twitch.outbound().next("PONG :tmi.twitch.tv"));
        
        Flux<TwitchMessage> messageRelay = twitch.inbound().ofType(IRCEvent.Message.class)
            .filter(e -> e.getChannel().equals(lcChan))
            .flatMap(e -> helper.getUser(e.getUser())
                                .zipWith(helper.getUser(e.getChannel()),
                                        (u, c) -> new TwitchMessage(twitch, e, c.displayName, u.displayName, u.avatarUrl)));
        
        return Flux.merge(pingPong, messageRelay).ofType(TwitchMessage.class);
    }
    
    @Override
    public Mono<TwitchMessage> send(String channel, ChatMessage<?> message, boolean raw) {
        String content = raw ? message.getContent() : message.toString();
        String username = TwitchService.getInstance().getData().getNick();
        return Mono.just(twitch.outbound())
                .doOnNext(sink -> sink.next("PRIVMSG #" + channel.toLowerCase(Locale.ROOT) + " :" + content))
                .thenReturn(new TwitchMessage(twitch, new IRCEvent.Message(ImmutableMap.of(), username, channel, content), channel, username, null)); // TODO have a second websocket reading our own message events
    }

    @Override
    public void disconnect(String channel) {
        twitch.outbound().next("PART #" + channel);
    }
}