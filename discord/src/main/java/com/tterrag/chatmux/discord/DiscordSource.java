package com.tterrag.chatmux.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.discord.util.WebhookMessage;
import com.tterrag.chatmux.links.LinkManager;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuples;

public class DiscordSource implements ChatSource<Dispatch, GatewayPayload<?>> {
    
    @NonNull
    private static final String ADMIN_EMOTE = "\u274C";
    private static final Pattern TEMP_COMMAND_PATTERN = Pattern.compile("^\\s*(\\+link(raw)?|^-link|^~links)");
    
    @NonNull
    private final DiscordClient client;
    
    @NonNull
    private final DiscordRequestHelper helper;
    
    DiscordSource(String token) {
        this.client = new DiscordClientBuilder(token).build();
        this.helper = new DiscordRequestHelper(client, token);
    }

    @Override
    public ChatService<Dispatch, GatewayPayload<?>> getType() {
        return DiscordService.getInstance();
    }
    
    @Override
    public Mono<String> parseChannel(String channel) {
        return Mono.fromSupplier(() -> Long.parseLong(channel))
                .thenReturn(channel)
                .onErrorResume(NumberFormatException.class, t -> Mono.just(DiscordMessage.CHANNEL_MENTION.matcher(channel))
                        .filter(Matcher::matches)
                        .map(m -> m.group(1))
                        .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("ChatChannel must be a mention or ID"))));
    }

    @Override
    public Flux<ChatMessage> connect(String channel) {
        // Discord bots do not "join" channels so we only need to return the flux of messages
        return client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(e -> e.getMember() != null)
                .filter(e -> e.getMessage().getChannelId().equals(Snowflake.of(channel)))
                .filter(e -> e.getMessage().getAuthor().map(u -> !u.isBot()).orElse(true))
                .filter(e -> e.getMessage().getContent().isPresent())
                .filter(e -> !TEMP_COMMAND_PATTERN.matcher(e.getMessage().getContent().get()).find())
                .flatMap(e -> e.getMessage().getChannel().ofType(TextChannel.class).map(c -> Tuples.of(e, c)))
                .flatMap(t -> DiscordMessage.create(client, t.getT1().getMessage()));
    }
    
    @Override
    public Mono<Void> send(String channelName, ChatMessage m, boolean raw) {
        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        Snowflake channel = Snowflake.of(channelName);
        String usercheck = m.getUser() + " (" + m.getSource() + "/" + m.getChannel() + ")";
        if (usercheck.length() > 32) {
            usercheck = m.getUser() + " (" + m.getSource().getName().substring(0, 1).toUpperCase(Locale.ROOT) + "/" + m.getChannel() + ")";
        }
        final String username = usercheck;
        return helper.getWebhook(channel, "ChatMux", in)
                    .flatMap(wh -> helper.executeWebhook(wh, new WebhookMessage(m instanceof DiscordMessage ? ((DiscordMessage)m).getRawContent() : m.getContent(), username, m.getAvatar()).toString())).map(r -> Tuples.of(m, r))
                    .flatMap(t -> client.getChannelById(channel).flatMap(c -> DiscordMessage.create(client, t.getT2()).doOnNext(msg -> LinkManager.INSTANCE.linkMessage(t.getT1(), msg))).thenReturn(t))
                    .filter(t -> !Main.cfg.getModerators().isEmpty() || !Main.cfg.getAdmins().isEmpty())
                    .flatMap(t -> t.getT2().addReaction(ReactionEmoji.unicode(ADMIN_EMOTE)).thenReturn(t))
                    .flatMap(t -> client.getEventDispatcher().on(ReactionAddEvent.class)
                            .take(Duration.ofSeconds(5))
                            .filterWhen(mra -> client.getSelf().map(User::getId).map(id -> !id.equals(mra.getUserId())))
                            .filter(mra -> mra.getMessageId() == t.getT2().getId())
                            .filter(mra -> mra.getEmoji().asUnicodeEmoji().map(u -> u.getRaw().equals(ADMIN_EMOTE)).orElse(false))
                            .next()
                            .flatMap(mra -> mra.getMessage().flatMap(Message::delete).and(t.getT1().delete()).thenReturn(t.getT2()))
                            .switchIfEmpty(client.getSelf().flatMap(u -> t.getT2().removeReaction(ReactionEmoji.unicode(ADMIN_EMOTE), t.getT2().getAuthor().map(User::getId).get()).thenReturn(t.getT2()))))
                    .then();
    }

    @Override
    public void disconnect(String channel) {}

    public DiscordClient getClient() {
        return client;
    }
}