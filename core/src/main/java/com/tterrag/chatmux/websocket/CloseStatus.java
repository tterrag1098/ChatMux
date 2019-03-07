package com.tterrag.chatmux.websocket;

import lombok.Value;
import reactor.util.annotation.Nullable;

@Value
public class CloseStatus {
    
    int code;
    @Nullable String reason;

}
