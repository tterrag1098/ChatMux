package com.tterrag.chatmux.bridge.twitch;

import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.WebSocketClient;

public class TwitchMessage extends Message {
    
    private final WebSocketClient<?, String> client;
    private final IRCEvent.Message message;

    public TwitchMessage(WebSocketClient<?, String> client, IRCEvent.Message message) {
        super(ServiceType.TWITCH, message.getChannel(), message.getUser(), message.getContent());
        this.client = client;
        this.message = message;
    }

    @Override
    public void delete() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/delete " + message.getTags().get(IRCEvent.Message.Tag.id));
    }
    
    @Override
    public void kick() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/timeout " + message.getUser() + " 1");
    }
    
    @Override
    public void ban() {
        client.outbound().next("PRIVMSG #" + getChannel() + " :/ban " + message.getUser());
    }
}
