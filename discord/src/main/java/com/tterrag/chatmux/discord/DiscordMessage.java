package com.tterrag.chatmux.discord;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reactivestreams.Publisher;

import com.tterrag.chatmux.bridge.AbstractChatMessage;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class DiscordMessage extends AbstractChatMessage<DiscordMessage> {
    
    static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d+)>");
    static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d+)>");
    static final Pattern EMOTE = Pattern.compile("<(a)?:(\\S+):(\\d+)>");
    
    public static Mono<DiscordMessage> create(DiscordClient client, Message message) {
        return Mono.zip(
                    message.getGuild(),
                    message.getChannel().cast(TextChannel.class),
                    message.getAuthorAsMember())
                .flatMap(t -> stripAllMentions(client, message.getContent().get(), t.getT2())
                        .map(m -> new DiscordMessage(message.getContent().get(), m, t.getT1(), t.getT2(), t.getT3(), message)));
    }
    
    private static Mono<String> stripAllMentions(DiscordClient client, String content, TextChannel channel) {
        return Mono.just(content)
                .flatMap(s -> stripMentions(CHANNEL_MENTION, 1, client::getChannelById, Channel::getId, c -> "#" + ((GuildChannel)c).getName(), s))
                .flatMap(s -> stripMentions(USER_MENTION, 1, id -> client.getMemberById(channel.getGuildId(), id),
                        User::getId, r -> "@" + r.getDisplayName(), s))
                .flatMap(s -> stripMentions(ROLE_MENTION, 1, id -> client.getRoleById(channel.getGuildId(), id),
                        Role::getId, r -> "@" + r.getName(), s))
                .map(s -> EMOTE.matcher(s).replaceAll(":$2:"));
    }
    
    private static <T> Mono<String> stripMentions(Pattern pattern, int idGroup, Function<Snowflake, Publisher<T>> setup, Function<T, Snowflake> keyExtractor, Function<T, String> converter, String content) {
        Matcher m = pattern.matcher(content);
        Set<Snowflake> found = new HashSet<>();
        while (m.find()) {
            found.add(Snowflake.of(m.group(idGroup)));
        }
        return Flux.fromIterable(found)
                .flatMap(setup)
                .onErrorContinue((t, o) -> log.error("Exception stripping mentions", t))
                .collectMap(keyExtractor)
                .map(map -> {
                    Matcher m2 = pattern.matcher(content);
                    StringBuffer sb = new StringBuffer();
                    while (m2.find()) {
                        T res = map.get(Snowflake.of(m2.group(idGroup)));
                        m2.appendReplacement(sb, converter.apply(res));
                    }
                    m2.appendTail(sb);
                    return sb.toString();
                });
    }
    
    @Getter
    private final String rawContent;
    private final Guild guild;
    private final TextChannel channel;
    private final User author;
    private final Message message;
    
    private DiscordMessage(String rawContent, String content, Guild guild, TextChannel channel, Member author, Message msg) {
        super(DiscordService.getInstance(), "#" + channel.getName(), channel.getId().asString(), author.getDisplayName(), content, author.getAvatarUrl());
        this.rawContent = rawContent;
        this.guild = guild;
        this.channel = channel;
        this.author = author;
        this.message = msg;
    }

    @Override
    public Mono<Void> delete() {
        return message.delete();
    }

    @Override
    public Mono<Void> kick() {
        return guild.kick(author.getId());
    }

    @Override
    public Mono<Void> ban() {
        return guild.ban(author.getId(), ban -> ban.setDeleteMessageDays(0));
    }
}
