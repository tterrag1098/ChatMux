package com.tterrag.chatmux.api.bot;

import java.util.Set;

import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.command.CommandHandler;
import com.tterrag.chatmux.api.link.LinkManager;

import reactor.core.publisher.Mono;

public interface BotInterface {
    
    Mono<CommandHandler> getCommandHandler(LinkManager manager);
    
    ChatService<?> getService();
    
    Set<String> getAdmins();

}
