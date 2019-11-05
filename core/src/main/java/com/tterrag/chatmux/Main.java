package com.tterrag.chatmux;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.api.config.ServiceData;
import com.tterrag.chatmux.api.wiretap.WiretapPlugin;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.links.LinkManager;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();
    
    public static void main(String[] args) {
    	start(null).block();
    }
    
    public static Mono<Void> start(@Nullable Path pluginRoot) {
        PluginManager pluginManager = new DefaultPluginManager(pluginRoot == null ? Paths.get("plugins") : pluginRoot);
        log.info("Using plugin folder: " + pluginManager.getPluginsRoot().toAbsolutePath());
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<ChatService<?>> services = (List) pluginManager.getExtensions(AbstractChatService.class);
        log.info("Loaded services: {}", services);

        // Load config after plugins so that AbstractChatService converter works
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        for (ChatService<?> service : services) {
            log.info("Connecting to service: {}", service);
            @SuppressWarnings("unchecked") 
            ServiceConfig<ServiceData> config = (ServiceConfig<ServiceData>) service.getConfig();
            if (config != null) {
            	config.onLoad(cfgReader.get(service.getName(), config::makeDefault));
            }
        }
        
        Hooks.onOperatorDebug();
        
        services.forEach(ChatService::initialize);
        
        List<WiretapPlugin> wiretaps = pluginManager.getExtensions(WiretapPlugin.class);
        log.info("Loaded wiretaps: {}", wiretaps);
        
        log.info("Delegating to main interface: {}", Main.cfg.getMain());
        return Main.cfg.getMain().runInterface(new LinkManager(wiretaps));
    }
}
