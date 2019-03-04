package com.tterrag.chatmux.links;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.tterrag.chatmux.bridge.discord.DiscordMessage;
import com.tterrag.chatmux.bridge.discord.DiscordRequestHelper;
import com.tterrag.chatmux.bridge.factorio.FactorioClient;
import com.tterrag.chatmux.bridge.factorio.FactorioMessage;
import com.tterrag.chatmux.bridge.mixer.MixerMessage;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.twitch.TwitchMessage;
import com.tterrag.chatmux.bridge.twitch.TwitchRequestHelper;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

public interface ChatSource<I, O> {
    
    public ServiceType<I, O> getType();
    
    public Flux<? extends Message> connect(WebSocketClient<I, O> client, String channel);
    
    @RequiredArgsConstructor
    class Discord implements ChatSource<Dispatch, GatewayPayload<?>> {
        
        private static final Pattern TEMP_COMMAND_PATTERN = Pattern.compile("^\\s*(\\+link(raw)?|^-link|^~links)");
        
        private final DiscordRequestHelper helper;

        @Override
        public ServiceType<Dispatch, GatewayPayload<?>> getType() {
            return ServiceType.DISCORD;
        }

        @Override
        public Flux<Message> connect(WebSocketClient<Dispatch, GatewayPayload<?>> client, String channel) {
            // Discord bots do not "join" channels so we only need to return the flux of messages
            return client.inbound()
                    .ofType(MessageCreate.class)
                    .filter(e -> e.getMember() != null)
                    .filter(e -> e.getChannelId() == Long.parseLong(channel))
                    .filter(e -> { Boolean bot = e.getAuthor().isBot(); return bot == null || !bot; })
                    .filter(e -> e.getContent() != null && !TEMP_COMMAND_PATTERN.matcher(e.getContent()).find())
                    .flatMap(e -> helper.getChannel(e.getChannelId()).map(c -> Tuples.of(e, c)))
                    .map(t -> new DiscordMessage(helper, t.getT2().getName(), t.getT1()));
        }
    }
    
    @RequiredArgsConstructor
    class Mixer implements ChatSource<MixerEvent, MixerMethod> {
        
        private final MixerRequestHelper helper;

        @Override
        public ServiceType<MixerEvent, MixerMethod> getType() {
            return ServiceType.MIXER;
        }
        
        @Override
        public Flux<Message> connect(WebSocketClient<MixerEvent, MixerMethod> client, String channel) {
            return client.inbound()
                .ofType(MixerEvent.Message.class)
                .flatMap(e -> helper.getUser(e.userId)
                                    .map(u -> new MixerMessage(helper, client, e, u.avatarUrl)));
        }
    }
    
    @RequiredArgsConstructor
    class Twitch implements ChatSource<IRCEvent, String> {
        
        private final TwitchRequestHelper helper;

        @Override
        public ServiceType<IRCEvent, String> getType() {
            return ServiceType.TWITCH;
        }
        
        @Override
        public Flux<Message> connect(WebSocketClient<IRCEvent, String> client, String channel) {
            final String lcChan = channel.toLowerCase(Locale.ROOT);
            client.outbound().next("JOIN #" + lcChan);
            
            return client.inbound()
                .ofType(IRCEvent.Message.class)
                .filter(e -> e.getChannel().equals(lcChan))
                .flatMap(e -> helper.getUsers(e.getUser())
                                    .flatMapMany(Flux::fromArray)
                                    .next()
                                    .map(u -> new TwitchMessage(client, e, u.displayName, u.avatarUrl)));
        }
    }
    
    class Factorio implements ChatSource<FactorioMessage, String> {
        
        @Override
        public ServiceType<FactorioMessage, String> getType() {
            return ServiceType.FACTORIO;
        }
        
        @Override
        public Flux<FactorioMessage> connect(WebSocketClient<FactorioMessage, String> client, String channel) {
            return client.inbound()
                    .filter(m -> FactorioClient.GLOBAL_TEAM.equals(channel) || m.getChannel().equals(channel));
        }
    }
}
