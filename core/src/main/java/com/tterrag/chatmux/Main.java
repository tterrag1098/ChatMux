package com.tterrag.chatmux;

import java.util.ServiceLoader;

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
        
        ServiceLoader.load(ChatService.class);

        Main.cfg.getMain().runInterface().block();
    }
}
