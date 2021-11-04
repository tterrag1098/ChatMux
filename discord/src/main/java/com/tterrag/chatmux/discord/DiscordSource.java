package com.tterrag.chatmux.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatSource;
import com.tterrag.chatmux.api.link.LinkManager;
import com.tterrag.chatmux.discord.command.DiscordCommandHandler;
import com.tterrag.chatmux.discord.util.WebhookMessage;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import emoji4j.EmojiUtils;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuples;

public class DiscordSource implements ChatSource<DiscordMessage> {
    
    @NonNull
    private static final String ADMIN_EMOTE = "\u274C";
    
    private static final Pattern MENTION = Pattern.compile("(?:^|[^\\\\])@(\\S+)");
    private static final Pattern CHANNEL = Pattern.compile("#(\\S+)");
    private static final Pattern EMOTE = Pattern.compile(":(\\S+):");
    
    @NonNull
    @Getter
    private final DiscordClient client;
    
    @NonNull
    private Optional<DiscordCommandHandler> commandHandler = Optional.empty();
    
    @NonNull
    private final DiscordRequestHelper helper;
    
    @NonNull
    private final Flux<MessageCreateEvent> messageSource;
    
    DiscordSource(String token) {
        this.client = new DiscordClientBuilder(token).build();
        this.helper = new DiscordRequestHelper(client, token);

        this.messageSource = client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(e -> e.getMember().isPresent())
                .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(true))
                .filter(e -> e.getMessage().getContent().isPresent())
                .filterWhen(e -> commandHandler.map(ch -> ch.canHandle(DiscordService.getInstance(), e.getMessage().getContent().get()))
                        .orElse(Mono.just(Boolean.FALSE))
                        .map(t -> !t))
                .share();
    }

    @Override
    public DiscordService getType() {
        return DiscordService.getInstance();
    }

    @Override
    public Flux<DiscordMessage> connect(String channel) {
        // Discord bots do not "join" channels so we only need to return the flux of messages
        return messageSource
                .filter(e -> e.getMessage().getChannelId().asString().equals(channel))
                .flatMap(e -> e.getMessage().getChannel().ofType(TextChannel.class).map(c -> Tuples.of(e, c)))
                .flatMap(t -> DiscordMessage.create(t.getT1().getMessage()));
    }
    
    @Override
    public Mono<DiscordMessage> send(String channelName, ChatMessage<?> m, boolean raw) {
        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        Snowflake channel = Snowflake.of(channelName);
        String usercheck = m.getUser() + " (" + m.getService() + "/" + m.getChannel() + ")";
        if (usercheck.length() > 32) {
            usercheck = m.getUser() + " (" + m.getService().getName().substring(0, 1).toUpperCase(Locale.ROOT) + "/" + m.getChannel() + ")";
        }
        final String username = usercheck;
        return helper.getWebhook(channel, "ChatMux", in)
                    .flatMap(wh -> discordify(channel, m).flatMap(msg -> helper.executeWebhook(wh, new WebhookMessage(msg, username, m.getAvatar()).toString()))).map(r -> Tuples.of(m, r))
                    .filter(t -> (!Main.cfg.getModerators().isEmpty() || !Main.cfg.getAdmins().isEmpty()) && DiscordService.getInstance().getData().getModerationChannels().contains(t.getT2().getChannelId().asLong()))
                    .flatMap(t -> t.getT2().addReaction(ReactionEmoji.unicode(ADMIN_EMOTE)).thenReturn(t))
                    .flatMap(t -> client.getEventDispatcher().on(ReactionAddEvent.class)
                            .take(Duration.ofSeconds(5))
                            .filterWhen(mra -> client.getSelf().map(User::getId).map(id -> !id.equals(mra.getUserId())))
                            .filter(mra -> mra.getMessageId() == t.getT2().getId())
                            .filter(mra -> mra.getEmoji().asUnicodeEmoji().map(u -> u.getRaw().equals(ADMIN_EMOTE)).orElse(false))
                            .next()
                            .flatMap(mra -> mra.getMessage().flatMap(Message::delete).and(t.getT1().delete()).thenReturn(t.getT2()))
                            .switchIfEmpty(Mono.justOrEmpty(client.getSelfId()).flatMap(u -> t.getT2().removeReaction(ReactionEmoji.unicode(ADMIN_EMOTE), u)).thenReturn(t.getT2())))
                    .flatMap(msg -> DiscordMessage.create(msg));
    }

    private Mono<String> discordify(Snowflake channel, ChatMessage<?> msg) {
        if (msg instanceof DiscordMessage) {
            return Mono.just(((DiscordMessage) msg).getRawContent());
        }
        return client.getChannelById(channel)
                .ofType(TextChannel.class)
                .flatMap(c -> c.getGuild())
                .flatMap(g -> parseMentions(msg.getContent(), g)
                        .flatMap(s -> parseChannels(s, g))
                        .flatMap(s -> parseEmotes(s, g)));
    }
    
    private Mono<String> parseMentions(String content, Guild guild) {
        return parse(MENTION, 1, guild::getMembers,
                (found, m) -> found.contains(m.getDisplayName().toLowerCase(Locale.ROOT)) || found.contains(m.getUsername().toLowerCase(Locale.ROOT)),
                (map, m) -> {
                    map.put(m.getDisplayName().toLowerCase(Locale.ROOT), m);
                    map.put(m.getUsername().toLowerCase(Locale.ROOT), m);
                },
                m -> "<@" + m.getId().asString() + ">",
                content, guild);
    }
    
    private Mono<String> parseChannels(String content, Guild guild) {
        return parse(CHANNEL, 1, guild::getChannels,
                (found, c) -> found.contains(c.getName().toLowerCase(Locale.ROOT)),
                (map, c) -> map.put(c.getName().toLowerCase(Locale.ROOT), c),
                c -> "<#" + c.getId().asString() + ">",
                content, guild);
    }
    
    private Mono<String> parseEmotes(String content, Guild guild) {
        return parse(EMOTE, 1, guild::getEmojis,
                (found, e) -> found.contains(e.getName().toLowerCase(Locale.ROOT)),
                (map, e) -> map.put(e.getName().toLowerCase(Locale.ROOT), e),
                e -> (e.isAnimated() ? "<a:" : "<:") + e.getName() + ":" + e.getId().asString() + ">",
                content, guild)
                .map(EmojiUtils::emojify);
    }
    
    private <T> Mono<String> parse(Pattern pattern, int group, Supplier<Flux<T>> start, BiPredicate<Collection<String>, T> matches, BiConsumer<Map<String, T>, T> collector, Function<T, String> converter, String content, Guild guild) {
        Matcher m = pattern.matcher(content);
        Set<String> found = new HashSet<>();
        while (m.find()) {
            found.add(m.group(group).toLowerCase(Locale.ROOT));
        }
        if (found.isEmpty()) {
            return Mono.just(content);
        }
        return start.get()
                .filter(member -> matches.test(found, member))
                .collect(() -> new HashMap<String, T>(), collector)
                .map(map -> {
                   Matcher m2 = pattern.matcher(content);
                   StringBuffer sb = new StringBuffer();
                   while (m2.find()) {
                       T val = map.get(m2.group(group).toLowerCase(Locale.ROOT));
                       if (val != null) {
                           m2.appendReplacement(sb, converter.apply(val));
                       }
                   }
                   m2.appendTail(sb);
                   return sb.toString();
                });
    }

    @Override
    public void disconnect(String channel) {}
    
    public DiscordCommandHandler createCommandHandler(LinkManager manager) {
        return (this.commandHandler = Optional.of(new DiscordCommandHandler(client, manager))).get(); 
    }
}