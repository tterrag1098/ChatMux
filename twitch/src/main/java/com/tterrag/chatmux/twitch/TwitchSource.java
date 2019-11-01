package com.tterrag.chatmux.twitch;

import java.util.Locale;
import java.util.function.Function;

import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.twitch.irc.IRCEvent;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
public class TwitchSource implements ChatSource {
    
    private final TwitchRequestHelper helper;
    private boolean connected;

    @NonNull
    private final WebSocketClient<IRCEvent, String> twitch = new SimpleWebSocketClient<>();
    
    @Override
    public ChatService getType() {
        return TwitchService.getInstance();
    }
    
    @Override
    public Flux<ChatMessage> connect(String channel) {
        if (!connected) {
            twitch.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
                .subscribe();
            
            twitch.outbound()
                .next("PASS oauth:" + TwitchService.getInstance().getData().getToken())
                .next("NICK " + TwitchService.getInstance().getData().getNick())
                .next("CAP REQ :twitch.tv/tags")
                .next("CAP REQ :twitch.tv/commands");
            connected = true;
        }
        final String lcChan = channel.toLowerCase(Locale.ROOT);
        twitch.outbound().next("JOIN #" + lcChan);
        
        return twitch.inbound().ofType(IRCEvent.Message.class)
            .filter(e -> e.getChannel().equals(lcChan))
            .flatMap(e -> helper.getUsers(e.getUser())
                                .flatMapMany(Flux::fromArray)
                                .next()
                                .map(u -> new TwitchMessage(twitch, e, u.displayName, u.avatarUrl)));
    }
    
    @Override
    public Mono<Void> send(String channel, ChatMessage message, boolean raw) {
        return Mono.just(twitch.outbound())
                .doOnNext(sink -> sink.next("PRIVMSG #" + channel.toLowerCase(Locale.ROOT) + " :" + (raw ? message.getContent() : message)))
                .then();
    }

    @Override
    public void disconnect(String channel) {
        twitch.outbound().next("PART #" + channel);
    }
}