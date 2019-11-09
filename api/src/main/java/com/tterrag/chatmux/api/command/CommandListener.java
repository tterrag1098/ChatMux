package com.tterrag.chatmux.api.command;

import org.pf4j.ExtensionPoint;

import com.tterrag.chatmux.api.bridge.ChatMessage;

import reactor.core.publisher.Mono;

public interface CommandListener extends ExtensionPoint {
    
    <M extends ChatMessage<M>> Mono<?> runCommand(String command, CommandContext<M> ctx);

}
