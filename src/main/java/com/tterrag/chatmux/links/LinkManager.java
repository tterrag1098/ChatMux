package com.tterrag.chatmux.links;

import java.util.ArrayList;
import java.util.List;

import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.Disposable;

public class LinkManager {
    
    private final List<Disposable> subs = new ArrayList<>();
    
    protected <I, O> void connect(ChatSource<I, O> from, WebSocketClient<I, O> client, ChatSource<?, ?> to) {
        from.connect(client, channel)
    }

    public void addLink(Link link) {
        this.links.add(link);
        this.connect(link);
    }
}
