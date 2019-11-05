package com.tterrag.chatmux.api.wiretap;

import org.pf4j.ExtensionPoint;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;

import reactor.core.publisher.Mono;

public interface WiretapPlugin extends ExtensionPoint {

    /**
     * Invoked for every message that goes through a link. Message objects are guaranteed to be reference-comparable if
     * they come from the same source, i.e. one message on any service only ever creates one ChatMessage object.
     * <p>
     * However, this will be invoked multiple times if a channel is outwardly linked (going-to) multiple channels. In
     * that case, reference comparisons, or checks based on the {@code from} and {@code to} params can be done to avoid
     * duplicate actions.
     * 
     * @param <M>
     *            The type of the incoming message
     * @param msg
     *            The message being sent through the link
     * @param from
     *            {@link ChatChannel Channel} the message is coming from
     * @param to
     *            {@link ChatChannel Channel} the message is going to
     * @return A {@link Mono} which performs the required action
     */
    <M extends ChatMessage<M>> Mono<?> onMessage(M msg, ChatChannel<M> from, ChatChannel<?> to);

}
