package com.tterrag.chatmux.util.command;

import com.tterrag.chatmux.api.bridge.ChatMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CommandContext {
    
    @Getter
    private final ChatMessage<?> message;

}
