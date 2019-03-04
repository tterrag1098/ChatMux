package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;

import reactor.core.publisher.Mono;

public class FactorioMessage extends Message {
    boolean action;
    
    public FactorioMessage(String username, String team, String message, boolean action) {
        super(ServiceType.FACTORIO, team, team, username, message, null);
        this.action = action;
    }
    
    @Override
    public String getContent() {
        String content = super.getContent();
        if (action) {
            content = "*" + content + "*";
        }
        return content;
    }
    
    @Override
    public Mono<Void> delete() {
        return Mono.empty();  // Impossible
    }
    
    @Override
    public Mono<Void> kick() {
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> ban() {
        return Mono.empty();
    }

    @Override
    public String toString() {
        return "[" + getSource() + "] <" + getUser() + "> " + getContent();
    }
}