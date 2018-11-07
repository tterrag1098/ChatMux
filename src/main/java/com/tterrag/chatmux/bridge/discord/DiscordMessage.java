package com.tterrag.chatmux.bridge.discord;

import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;

import discord4j.common.json.MessageResponse;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.MessageReactionAdd;

public class DiscordMessage extends Message {
    
    private final DiscordRequestHelper helper;
    
    private final Long guild;
    private final long channel;
    private final long author;
    private final long id;

    public DiscordMessage(DiscordRequestHelper helper, String channelName, MessageCreate message) {
        this(helper, channelName, message.getAuthor().getUsername(), message.getContent(), message.getGuildId(), message.getChannelId(), message.getAuthor().getId(), message.getId());
    }
    
    public DiscordMessage(DiscordRequestHelper helper, String channelName, MessageResponse message, Long guildId) {
        this(helper, channelName, message.getAuthor().getUsername(), message.getContent(), guildId, message.getChannelId(), message.getAuthor().getId(), message.getId());
    }
    
    private DiscordMessage(DiscordRequestHelper helper, String channelName, String authorName, String content, Long guild, long channel, long author, long id) {
        super(ServiceType.DISCORD, "#" + channelName, Long.toString(channel), authorName, content);
        this.helper = helper;
        this.guild = guild;
        this.channel = channel;
        this.author = author;
        this.id = id;
    }

    @Override
    public void delete() {
        helper.deleteMessage(channel, id);
    }

    @Override
    public void kick() {
        Long guildId = guild;
        if (guildId != null) {
            helper.kick(guildId, author);
        }
    }

    @Override
    public void ban() {
        Long guildId = guild;
        if (guildId != null) {
            helper.ban(guildId, author, 0, "ChatMux ban");
        }
    }
}
