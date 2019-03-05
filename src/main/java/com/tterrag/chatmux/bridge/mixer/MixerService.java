package com.tterrag.chatmux.bridge.mixer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.util.Service;

import lombok.Getter;

public class MixerService extends Service<MixerEvent, MixerMethod> {
    
    private final MixerRequestHelper helper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final MixerSource source = new MixerSource(helper);

    public MixerService() {
        super("mixer");
    }
}
