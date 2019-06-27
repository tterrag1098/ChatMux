package com.tterrag.chatmux;

import java.util.List;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.config.ServiceConfig;
import com.tterrag.chatmux.config.ServiceData;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();
    
    public static void main(String[] args) {
    	start().block();
    }
    
    public static Mono<Void> start() {
        PluginManager pluginManager = new DefaultPluginManager();
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        @SuppressWarnings({ "unchecked", "rawtypes" }) 
        List<ChatService<?, ?>> services = (List) pluginManager.getExtensions(ChatService.class);

        // Load config after plugins so that ChatService converter works
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        for (ChatService<?, ?> service : services) {
            @SuppressWarnings("unchecked") 
            ServiceConfig<ServiceData> config = (ServiceConfig<ServiceData>) service.getConfig();
            config.onLoad(cfgReader.get(service.getName(), config::makeDefault));
        }
        
        Hooks.onOperatorDebug();
        
        services.forEach(ChatService::initialize);
        
        return Main.cfg.getMain().runInterface();
    }
}
