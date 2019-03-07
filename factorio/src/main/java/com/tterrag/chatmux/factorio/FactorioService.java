package com.tterrag.chatmux.factorio;

import org.pf4j.Extension;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.config.ServiceConfig;
import com.tterrag.chatmux.config.SimpleServiceConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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
    
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private FactorioData data = new FactorioData();
    
    @Override
    public ServiceConfig<?> getConfig() {
        return new SimpleServiceConfig<>(FactorioData::new, this::setData);
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
