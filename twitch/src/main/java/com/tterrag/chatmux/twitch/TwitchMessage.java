package com.tterrag.chatmux.twitch;

import com.tterrag.chatmux.bridge.AbstractChatMessage;
import com.tterrag.chatmux.twitch.irc.IRCEvent;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Mono;

public class TwitchMessage extends AbstractChatMessage<TwitchMessage> {
    
    private final WebSocketClient<?, String> client;
    private final IRCEvent.Message message;

    public TwitchMessage(WebSocketClient<?, String> client, IRCEvent.Message message, String channelName, String displayname, String avatar) {
        super(TwitchService.getInstance(), channelName, displayname, message.getContent(), avatar);
        this.client = client;
        this.message = message;
    }

    @Override
    public Mono<Void> delete() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/delete " + message.getTags().get(IRCEvent.Message.Tag.id));
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> kick() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/timeout " + message.getUser() + " 1");
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> ban() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/ban " + message.getUser());
        return Mono.empty();
    }
}
