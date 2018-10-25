package com.tterrag.chatmux.util.command;


@FunctionalInterface
public interface CommandReader<M> {
    
    CommandContext parseMessage(M message);

}
