package com.tterrag.chatmux.bridge.discord;

import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;

import discord4j.gateway.json.dispatch.MessageCreate;

public class DiscordMessage extends Message {
    
    private final DiscordRequestHelper helper;
    
    private final MessageCreate message;

    public DiscordMessage(DiscordRequestHelper helper, String channelName, MessageCreate message) {
        //                               TODO can this be reactive?
        super(ServiceType.DISCORD, "#" + channelName, message.getAuthor().getUsername(), message.getContent());
        this.helper = helper;
        this.message = message;
    }

    @Override
    public void delete() {
        helper.deleteMessage(message.getChannelId(), message.getId());
    }

    @Override
    public void kick() {
        Long guildId = message.getGuildId();
        if (guildId != null) {
            helper.kick(guildId, message.getAuthor().getId());
        }
    }

    @Override
    public void ban() {
        Long guildId = message.getGuildId();
        if (guildId != null) {
            helper.ban(guildId, message.getAuthor().getId(), 0, "ChatMux ban");
        }
    }
}
