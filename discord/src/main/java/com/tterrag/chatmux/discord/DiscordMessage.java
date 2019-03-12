package com.tterrag.chatmux.discord;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reactivestreams.Publisher;

import com.tterrag.chatmux.bridge.ChatMessage;

import discord4j.common.json.GuildEmojiResponse;
import discord4j.common.json.MessageResponse;
import discord4j.common.json.RoleResponse;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.rest.json.response.ChannelResponse;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiscordMessage extends ChatMessage {
    
    static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d+)>");
    static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d+)>");
    static final Pattern EMOTE = Pattern.compile("<(a)?:(\\S+):(\\d+)>");
    
    public static Mono<DiscordMessage> create(DiscordRequestHelper helper, String channelName, MessageCreate message) {
        return create(helper, channelName, message.getAuthor().getUsername(), message.getContent(), message.getGuildId(), message.getChannelId(), message.getAuthor().getId(), message.getId(), message.getAuthor().getAvatar());
    }

    public static Mono<DiscordMessage> create(DiscordRequestHelper helper, String channelName, MessageResponse message, Long guildId) {
        return create(helper, channelName, message.getAuthor().getUsername(), message.getContent(), guildId, message.getChannelId(), message.getAuthor().getId(), message.getId(), message.getAuthor().getAvatar());
    }
    
    private static Mono<DiscordMessage> create(DiscordRequestHelper helper, String channelName, String authorName, String content, Long guild, long channel, long author, long id, String avatar) {
        return stripAllMentions(helper, content, channel).map(c -> new DiscordMessage(helper, channelName, authorName, content, c, guild, channel, author, id, avatar));
    }
    
    private static Mono<String> stripAllMentions(DiscordRequestHelper helper, String content, long channel) {
        return Mono.just(content)
                .flatMap(s -> stripMentions(CHANNEL_MENTION, 1, helper::getChannel, ChannelResponse::getId, r -> "#" + r.getName(), s, channel))
                .flatMap(s -> stripMentions(USER_MENTION, 1, id -> helper.getChannel(channel)
                        .flatMap(r -> Mono.justOrEmpty(r.getGuildId()))
                        .flatMap(g -> helper.getMember(g, id)),
                        r -> r.getUser().getId(),
                        r -> {
                            String name = r.getNick();
                            if (name == null) {
                                name = r.getUser().getUsername();
                            }
                            return "@" + name;
                        }, s, channel))
                .flatMap(s -> stripMentions(ROLE_MENTION, 1, id -> helper.getChannel(channel)
                        .flatMap(r -> Mono.justOrEmpty(r.getGuildId()))
                        .flatMap(g -> helper.getRole(g, id)),
                        RoleResponse::getId, r -> "@" + r.getName(), s, channel))
                .flatMap(s -> stripMentions(EMOTE, 3, id -> helper.getChannel(channel)
                        .flatMap(r -> Mono.justOrEmpty(r.getGuildId()))
                        .flatMap(g -> helper.getEmote(g, id)),
                        GuildEmojiResponse::getId, r -> ":" + r.getName() + ":", s, channel));
    }
    
    private static <T> Mono<String> stripMentions(Pattern pattern, int idGroup, Function<Long, Publisher<T>> setup, Function<T, Long> keyExtractor, Function<T, String> converter, String content, long channel) {
        Matcher m = pattern.matcher(content);
        Set<Long> found = new HashSet<>();
        while (m.find()) {
            found.add(Long.parseLong(m.group(idGroup)));
        }
        return Flux.fromIterable(found)
                .flatMap(setup)
                .collectMap(keyExtractor)
                .map(map -> {
                    Matcher m2 = pattern.matcher(content);
                    StringBuffer sb = new StringBuffer();
                    while (m2.find()) {
                        T res = map.get(Long.parseLong(m2.group(idGroup)));
                        m2.appendReplacement(sb, converter.apply(res));
                    }
                    m2.appendTail(sb);
                    return sb.toString();
                });
    }

    private final DiscordRequestHelper helper;
    
    @Getter
    private final String rawContent;
    private final Long guild;
    private final long channel;
    private final long author;
    private final long id;
    
    private DiscordMessage(DiscordRequestHelper helper, String channelName, String authorName, String rawContent, String content, Long guild, long channel, long author, long id, String avatar) {
        super(DiscordService.getInstance(), "#" + channelName, Long.toString(channel), authorName, content, "https://cdn.discordapp.com/avatars/" + author + "/" + avatar + ".png");
        System.out.println(getAvatar());
        this.helper = helper;
        this.rawContent = rawContent;
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
