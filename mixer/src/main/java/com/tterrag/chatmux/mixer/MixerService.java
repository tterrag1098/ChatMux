package com.tterrag.chatmux.mixer;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.method.MixerMethod;

@Extension
public class MixerService extends ChatService<MixerEvent, MixerMethod> {

    public MixerService() {
        super("mixer");
        instance = this;
    }
    
    @Override
    protected ChatSource<MixerEvent, MixerMethod> createSource() {
        MixerRequestHelper helper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
        return new MixerSource(helper);
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
