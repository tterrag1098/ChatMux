package com.tterrag.chatmux.api.link;

import java.util.List;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface LinkManager {

    Mono<Void> readLinks();

    <M extends ChatMessage<M>> Disposable connect(ChatChannel<M> from, ChatChannel<?> to, boolean raw);

    Link addLink(ChatChannel<?> from, ChatChannel<?> to, boolean raw, Disposable subscriber);

    List<Link> removeLink(ChatChannel<?> from, ChatChannel<?> to);

    List<? extends Link> getLinks();

    void linkMessage(ChatMessage<?> source, ChatMessage<?> linked);

    <M extends ChatMessage<M>> List<ChatMessage<M>> getLinkedMessages(ChatService<M> type, String id);
}
