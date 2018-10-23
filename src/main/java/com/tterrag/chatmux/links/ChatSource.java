package com.tterrag.chatmux.links;

import java.util.Locale;

import javax.annotation.Nullable;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.discord.DiscordRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.bridge.mixer.response.ChatResponse;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

public interface ChatSource<I, O> {
    
    public ServiceType getType();
    
    public Flux<Message> connect(WebSocketClient<I, O> client, String channel);
    
    @RequiredArgsConstructor
    class Discord implements ChatSource<Dispatch, GatewayPayload<?>> {
        
        private final DiscordRequestHelper helper;

        @Override
        public ServiceType getType() {
            return ServiceType.DISCORD;
        }

        @Override
        public Flux<Message> connect(WebSocketClient<Dispatch, GatewayPayload<?>> client, String channel) {
            // Discord bots do not "join" channels so we only need to return the flux of messages
            return client.inbound()
                    .ofType(MessageCreate.class)
                    .filter(e -> e.getMember() != null)
                    .filter(e -> e.getChannelId() == Long.parseLong(channel))
                    .flatMap(e -> Mono.zip(Mono.just(e), helper.getChannel(e.getChannelId())))
                    .map(t -> new Message(getType(), "#" + t.getT2().name, t.getT1().getAuthor().getUsername() + "#" + t.getT1().getAuthor().getDiscriminator(), t.getT1().getContent()));
        }
    }
    
    @RequiredArgsConstructor
    class Mixer implements ChatSource<MixerEvent, MixerMethod> {
        
        private final @Nullable ChatResponse chat;

        @Override
        public ServiceType getType() {
            return ServiceType.MIXER;
        }
        
        @Override
        public Flux<Message> connect(WebSocketClient<MixerEvent, MixerMethod> client, String channel) {
            final ChatResponse chat = this.chat;
            // If null, this is a reuse of the same websocket/channel
            if (chat != null) {
                client.outbound().next(new MixerMethod(MethodType.AUTH, Integer.parseInt(channel), Main.cfg.getMixer().getUserId(), chat.authKey));
            }
            
            return client.inbound()
                .ofType(MixerEvent.Message.class)
                .map(e -> new Message(getType(), "" + e.channel, e.username, e.message.rawText()));
        }
    }
    
    class Twitch implements ChatSource<IRCEvent, String> {

        @Override
        public ServiceType getType() {
            return ServiceType.TWITCH;
        }
        
        @Override
        public Flux<Message> connect(WebSocketClient<IRCEvent, String> client, String channel) {
            final String lcChan = channel.toLowerCase(Locale.ROOT);
            client.outbound().next("JOIN #" + lcChan);
            
            return client.inbound()
                .ofType(IRCEvent.Message.class)
                .filter(e -> e.getChannel().equals(lcChan))
                .map(e -> new Message(getType(), e.getChannel(), e.getUser(), e.getContent()));
        }
    }
}
