package com.tterrag.chatmux.mixer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.method.MixerMethod;

import lombok.Getter;

public class MixerService extends ChatService<MixerEvent, MixerMethod> {
    
    private final MixerRequestHelper helper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final MixerSource source = new MixerSource(helper);

    public MixerService() {
        super("mixer");
        instance = this;
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
