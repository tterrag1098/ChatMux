package com.tterrag.chatmux.bridge;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.electronwill.nightconfig.core.conversion.Converter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tterrag.chatmux.bridge.discord.DiscordService;
import com.tterrag.chatmux.bridge.factorio.FactorioService;
import com.tterrag.chatmux.bridge.mixer.MixerService;
import com.tterrag.chatmux.bridge.twitch.TwitchService;

import lombok.Getter;

@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public abstract class ChatService<I, O> {
    
    public static final DiscordService DISCORD = new DiscordService();
    public static final FactorioService FACTORIO = new FactorioService();
    public static final MixerService MIXER = new MixerService();
    public static final TwitchService TWITCH = new TwitchService();

    private static final Map<String, ChatService<?, ?>> types = new HashMap<>();
    
    @Getter
    private final String name;
    
    protected ChatService(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        types.put(this.name, this);
    }
    
    public abstract ChatSource<I, O> getSource();
    
    @Override
    public String toString() {
        return name;
    }

    public static final ChatService<?, ?> byName(String name) {
        return types.get(name.toLowerCase(Locale.ROOT));
    }

    public static class Conv implements Converter<ChatService<?, ?>, String> {

        @Override
        public ChatService<?, ?> convertToField(String value) {
            return byName(value);
        }

        @Override
        public String convertFromField(ChatService<?, ?> value) {
            return value.name;
        }
    }
}

class Serializer extends JsonSerializer<ChatService<?, ?>> {

    @Override
    public void serialize(ChatService<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());
    }
}

class Deserializer extends JsonDeserializer<ChatService<?, ?>> {

    @Override
    public ChatService<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return ChatService.byName(p.getValueAsString());
    }
}
