package com.tterrag.chatmux.mixer;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.SimpleServiceConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Extension
public class MixerService extends AbstractChatService<MixerMessage, MixerSource> {

    public MixerService() {
        super("mixer");
        instance = this;
    }
    
    @Override
    protected MixerSource createSource() {
        MixerRequestHelper helper = new MixerRequestHelper(new ObjectMapper(), getData().getClientId(), getData().getToken());
        return new MixerSource(helper);
    }
    
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private MixerData data = new MixerData();
    
    @Override
    public ServiceConfig<?> getConfig() {
        return new SimpleServiceConfig<>(MixerData::new, this::setData);
    }
    
    private static MixerService instance;

    public static MixerService getInstance() {
        MixerService inst = instance;
        if (inst == null) {
            throw new IllegalStateException("Mixer service not initialized");
        }
        return inst;
    }
    
    @Override
    public Mono<String> parseChannel(String channel) {
        return Mono.fromSupplier(() -> Integer.parseInt(channel))
                .map(Object::toString)
                .onErrorResume(NumberFormatException.class, $ -> getSource().getHelper()
                        .getChannel(channel)
                        .map(c -> Integer.toString(c.id)));
    }
    
    @Override
    public Mono<String> prettifyChannel(ChatService<?> target, ChatChannel<?> channel) {
        return getSource().getHelper()
                .getChannel(channel.getName())
                .flatMap(c -> getSource().getHelper().getUser(c.userId))
                .map(u -> channel.getService().getName() + "/" + u.username);
    }
}
