package com.tterrag.chatmux.factorio;

import org.pf4j.Extension;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;

@Extension
public class FactorioService extends ChatService<FactorioMessage, String> {
    
    public FactorioService() {
        super("factorio");
        instance = this;
    }
    
    @Override
    protected ChatSource<FactorioMessage, String> createSource() {
        return new FactorioSource();
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
