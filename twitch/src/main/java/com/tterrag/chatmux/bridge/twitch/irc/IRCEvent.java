package com.tterrag.chatmux.bridge.twitch.irc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

import lombok.EqualsAndHashCode;
import lombok.Value;

public class IRCEvent {
    
    private static final Pattern MESSAGE = Pattern.compile("^(?:@(.*)\\s)?:(\\w+)!\\w+@\\w+\\.tmi\\.twitch\\.tv PRIVMSG #(\\w+) :(.+)$");
    
    public static IRCEvent parse(String raw) {
        if (raw.equals("PING :tmi.twitch.tv")) {
            return new Ping();
        }
        
        Matcher m = MESSAGE.matcher(raw.trim());
        if (m.matches()) {
            String tags = m.group(1);
            ImmutableMap<Message.Tag, String> tagMap;
            if (tags == null) {
                tagMap = ImmutableMap.of();
            } else {
                ImmutableMap.Builder<Message.Tag, String> builder = ImmutableMap.builder();
                String[] tagArr = tags.split(";");
                for (String tag : tagArr) {
                    String[] tagData = tag.split("=");
                    try {
                        Message.Tag tagId = Message.Tag.valueOf(tagData[0]);
                        builder.put(tagId, tagData[1]);
                    } catch (IllegalArgumentException e) {}
                }
                tagMap = builder.build();
            }
            return new Message(tagMap, m.group(2), m.group(3), m.group(4));
        }
        
        return new IRCEvent(); // Unknown event
    }

    public static class Ping extends IRCEvent {}

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Message extends IRCEvent {
        
        public enum Tag {
            id,
            ;
        }
        
        private final ImmutableMap<Tag, String> tags;
        
        private final String user, channel, content;
    
    }

}
