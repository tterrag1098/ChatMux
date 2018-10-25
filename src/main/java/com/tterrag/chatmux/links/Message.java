package com.tterrag.chatmux.links;

import com.tterrag.chatmux.util.ServiceType;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public abstract class Message {
    
    ServiceType source;
    String channel;

    String user;
    String content;
    
    /**
     * Deletes the current message, exact behavior is up to the specific service.
     */
    public abstract void delete();
    
    /**
     * Kicks the user. Exact behavior may vary, for instance on twitch this equates to a "purge".
     */
    public abstract void kick();
    
    /**
     * Ban the author of this message
     */
    public abstract void ban();
    
    @Override
    public String toString() {
        return "[" + source + "/" + channel + "] <" + user + "> " + content;
    }
}
