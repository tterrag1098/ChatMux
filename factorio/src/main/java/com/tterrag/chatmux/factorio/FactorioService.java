package com.tterrag.chatmux.factorio;

import org.pf4j.Extension;

import com.tterrag.chatmux.bridge.ChatService;

import lombok.Getter;

@Extension
public class FactorioService extends ChatService<FactorioMessage, String> {
    
    @Getter(onMethod = @__({@Override}))
    private final FactorioSource source = new FactorioSource();

    public FactorioService() {
        super("factorio");
        instance = this;
    }
    
    private static FactorioService instance;

    public static FactorioService getInstance() {
        FactorioService inst = instance;
        if (inst == null) {
            throw new IllegalStateException("Factorio service not initialized");
        }
        return inst;
    }
}
