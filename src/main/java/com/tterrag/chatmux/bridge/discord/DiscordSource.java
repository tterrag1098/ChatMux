package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.regex.Pattern;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.util.WebhookMessage;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;

import discord4j.common.json.UserResponse;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.MessageReactionAdd;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuples;

@RequiredArgsConstructor
public class DiscordSource implements ChatSource<Dispatch, GatewayPayload<?>> {
    
    private static final String ADMIN_EMOTE = "\u274C";
    private static final Pattern TEMP_COMMAND_PATTERN = Pattern.compile("^\\s*(\\+link(raw)?|^-link|^~links)");
    
    private final DiscordRequestHelper helper;
    
    @NonNull
    private final DecoratedGatewayClient ws = new DecoratedGatewayClient();

    @Override
    public ChatService<Dispatch, GatewayPayload<?>> getType() {
        return ChatService.DISCORD;
    }

    @Override
    public Flux<ChatMessage> connect(String channel) {
        // Discord bots do not "join" channels so we only need to return the flux of messages
        return ws.inbound()
                .ofType(MessageCreate.class)
                .filter(e -> e.getMember() != null)
                .filter(e -> e.getChannelId() == Long.parseLong(channel))
                .filter(e -> { Boolean bot = e.getAuthor().isBot(); return bot == null || !bot; })
                .filter(e -> !TEMP_COMMAND_PATTERN.matcher(e.getContent()).find())
                .flatMap(e -> helper.getChannel(e.getChannelId()).map(c -> Tuples.of(e, c)))
                .map(t -> new DiscordMessage(helper, t.getT2().getName(), t.getT1()));
    }
    
    @Override
    public Mono<Void> send(String channelName, ChatMessage m, boolean raw) {
        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        long channel = Long.parseLong(channelName);
        return helper.getWebhook(channel, "ChatMux", in).flatMap(wh -> helper.executeWebhook(wh, new WebhookMessage(m.getContent(), m.getUser() + " (" + m.getSource() + "/" + m.getChannel() + ")", m.getAvatar()).toString())).map(r -> Tuples.of(m, r))
                    .flatMap(t -> helper.getChannel(channel).doOnNext(c -> LinkManager.INSTANCE.linkMessage(t.getT1(), new DiscordMessage(helper, Long.toString(channel), t.getT2(), c.getGuildId()))).thenReturn(t))
                    .filter(t -> !Main.cfg.getModerators().isEmpty() || !Main.cfg.getAdmins().isEmpty())
                    .flatMap(t -> helper.addReaction(t.getT2().getChannelId(), t.getT2().getId(), null, ADMIN_EMOTE).thenReturn(t))
                    .flatMap(t -> ws.inbound().ofType(MessageReactionAdd.class)
                            .take(Duration.ofSeconds(5))
                            .filterWhen(mra -> helper.getOurUser().map(UserResponse::getId).map(id -> id != mra.getUserId()))
                            .filter(mra -> mra.getMessageId() == t.getT2().getId())
                            .filter(mra -> mra.getEmoji().getName().equals(ADMIN_EMOTE))
                            .next()
                            .flatMap(mra -> helper.deleteMessage(mra.getChannelId(), mra.getMessageId()).and(t.getT1().delete()).thenReturn(t.getT2()))
                            .switchIfEmpty(helper.getOurUser().flatMap(u -> helper.removeReaction(t.getT2().getChannelId(), u.getId(), t.getT2().getId(), null, ADMIN_EMOTE)).thenReturn(t.getT2())))
                    .then();
    }
    
    @Override
    public void disconnect(String channel) {}

    public DecoratedGatewayClient getClient() {
        return ws;
    }
}