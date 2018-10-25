package com.tterrag.chatmux.util.command;

import com.tterrag.chatmux.links.Message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CommandContext {
    
    @Getter
    private final Message message;

}
