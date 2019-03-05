package com.tterrag.chatmux.bridge.discord;

import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.Service;

import discord4j.common.json.MessageResponse;
import discord4j.gateway.json.dispatch.MessageCreate;
import reactor.core.publisher.Mono;

public class DiscordMessage extends Message {
    
    private final DiscordRequestHelper helper;
    
    private final Long guild;
    private final long channel;
    private final long author;
    private final long id;

    public DiscordMessage(DiscordRequestHelper helper, String channelName, MessageCreate message) {
        this(helper, channelName, message.getAuthor().getUsername(), message.getContent(), message.getGuildId(), message.getChannelId(), message.getAuthor().getId(), message.getId(), message.getAuthor().getAvatar());
    }
    
    public DiscordMessage(DiscordRequestHelper helper, String channelName, MessageResponse message, Long guildId) {
        this(helper, channelName, message.getAuthor().getUsername(), message.getContent(), guildId, message.getChannelId(), message.getAuthor().getId(), message.getId(), message.getAuthor().getAvatar());
    }
    
    private DiscordMessage(DiscordRequestHelper helper, String channelName, String authorName, String content, Long guild, long channel, long author, long id, String avatar) {
        super(Service.DISCORD, "#" + channelName, Long.toString(channel), authorName, content, "https://cdn.discordapp.com/avatars/" + author + "/" + avatar + ".png");
        System.out.println(getAvatar());
        this.helper = helper;
        this.guild = guild;
        this.channel = channel;
        this.author = author;
        this.id = id;
    }

    @Override
    public Mono<Void> delete() {
        return helper.deleteMessage(channel, id);
    }

    @Override
    public Mono<Void> kick() {
        Long guildId = guild;
        if (guildId != null) {
            return helper.kick(guildId, author);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> ban() {
        Long guildId = guild;
        if (guildId != null) {
            return helper.ban(guildId, author, 0, "ChatMux ban");
        }
        return Mono.empty();
    }
}
