package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.bridge.ChatService;

import lombok.Getter;

public class FactorioService extends ChatService<FactorioMessage, String> {
    
    @Getter(onMethod = @__({@Override}))
    private final FactorioSource source = new FactorioSource();

    public FactorioService() {
        super("factorio");
    }
}
