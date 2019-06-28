package com.tterrag.chatmux.mixer;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.config.ServiceConfig;
import com.tterrag.chatmux.config.SimpleServiceConfig;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.method.MixerMethod;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Extension
public class MixerService extends ChatService {

    public MixerService() {
        super("mixer");
        instance = this;
    }
    
    @Override
    protected ChatSource createSource() {
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
}
