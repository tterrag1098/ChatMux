package com.tterrag.chatmux.api.command;

import java.util.Set;

import org.pf4j.ExtensionPoint;

import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.Connectable;

import reactor.core.publisher.Mono;

public interface CommandListener extends ExtensionPoint {
    
    default Mono<?> onServiceAvailable(Connectable connectable) {
        return Mono.empty();
    }
    
    <M extends ChatMessage<M>> Mono<?> runCommand(String command, CommandContext<M> ctx);

    Mono<Boolean> canHandle(String command, String args);

    default void setAdmins(Set<String> admins) {
    }
}
