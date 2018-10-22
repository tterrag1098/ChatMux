package com.tterrag.chatmux.bridge.twitch.irc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Value;

public class IRCEvent {
    
    private static final Pattern MESSAGE = Pattern.compile("^:(\\w+)!\\w+@\\w+\\.tmi\\.twitch\\.tv PRIVMSG #(\\w+) :(.+)$");
    
    public static IRCEvent parse(String raw) {
        if (raw.equals("PING :tmi.twitch.tv")) {
            return new Ping();
        }
        
        Matcher m = MESSAGE.matcher(raw.trim());
        if (m.matches()) {
            return new Message(m.group(1), m.group(2), m.group(3));
        }
        
        return new IRCEvent(); // Unknown event
    }

    public static class Ping extends IRCEvent {}

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Message extends IRCEvent {
        
        private final String user, channel, content;
    
    }

}
