package com.tterrag.chatmux.bridge.discord;

import java.util.regex.Pattern;

import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

@RequiredArgsConstructor
public
class DiscordSource implements ChatSource<Dispatch, GatewayPayload<?>> {
    
    private static final Pattern TEMP_COMMAND_PATTERN = Pattern.compile("^\\s*(\\+link(raw)?|^-link|^~links)");
    
    private final DiscordRequestHelper helper;

    @Override
    public ChatService<Dispatch, GatewayPayload<?>> getType() {
        return ChatService.DISCORD;
    }

    @Override
    public Flux<ChatMessage> connect(WebSocketClient<Dispatch, GatewayPayload<?>> client, String channel) {
        // Discord bots do not "join" channels so we only need to return the flux of messages
        return client.inbound()
                .ofType(MessageCreate.class)
                .filter(e -> e.getMember() != null)
                .filter(e -> e.getChannelId() == Long.parseLong(channel))
                .filter(e -> { Boolean bot = e.getAuthor().isBot(); return bot == null || !bot; })
                .filter(e -> !TEMP_COMMAND_PATTERN.matcher(e.getContent()).find())
                .flatMap(e -> helper.getChannel(e.getChannelId()).map(c -> Tuples.of(e, c)))
                .map(t -> new DiscordMessage(helper, t.getT2().getName(), t.getT1()));
    }
}