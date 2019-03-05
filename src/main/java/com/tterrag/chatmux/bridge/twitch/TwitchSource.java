package com.tterrag.chatmux.bridge.twitch;

import java.util.Locale;

import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.links.ChatSource;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.Service;
import com.tterrag.chatmux.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public
class TwitchSource implements ChatSource<IRCEvent, String> {
    
    private final TwitchRequestHelper helper;

    @Override
    public Service<IRCEvent, String> getType() {
        return Service.TWITCH;
    }
    
    @Override
    public Flux<Message> connect(WebSocketClient<IRCEvent, String> client, String channel) {
        final String lcChan = channel.toLowerCase(Locale.ROOT);
        client.outbound().next("JOIN #" + lcChan);
        
        return client.inbound()
            .ofType(IRCEvent.Message.class)
            .filter(e -> e.getChannel().equals(lcChan))
            .flatMap(e -> helper.getUsers(e.getUser())
                                .flatMapMany(Flux::fromArray)
                                .next()
                                .map(u -> new TwitchMessage(client, e, u.displayName, u.avatarUrl)));
    }
}