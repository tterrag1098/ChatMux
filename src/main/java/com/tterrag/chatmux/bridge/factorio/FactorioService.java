package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.util.Service;

import lombok.Getter;

public class FactorioService extends Service<FactorioMessage, String> {
    
    @Getter(onMethod = @__({@Override}))
    private final FactorioSource source = new FactorioSource();

    public FactorioService() {
        super("factorio");
    }
}
