package com.tterrag.chatmux;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();
    
    public static void main(String[] args) throws InterruptedException {        
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        Hooks.onOperatorDebug();
        
        PluginManager pluginManager = new DefaultPluginManager();
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        
        pluginManager.getExtensions(ChatService.class);

        Main.cfg.getMain().runInterface().block();
    }
}
