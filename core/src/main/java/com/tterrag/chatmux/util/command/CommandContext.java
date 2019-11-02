package com.tterrag.chatmux.util.command;

import com.tterrag.chatmux.bridge.AbstractChatMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CommandContext {
    
    @Getter
    private final AbstractChatMessage message;

}
