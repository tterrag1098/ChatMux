package com.tterrag.chatmux.api.command;

import reactor.core.publisher.Mono;

public interface CommandHandler {
    
    void addListener(CommandListener listener);

    Mono<Void> start();
}
