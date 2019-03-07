package com.tterrag.chatmux.websocket;


@SuppressWarnings("serial")
public class CloseException extends RuntimeException {
    
    public CloseException(CloseStatus status) {
        super(status.toString());
    }
    
    public CloseException(CloseStatus status, Throwable cause) {
        super(status.toString(), cause);
    }
}
