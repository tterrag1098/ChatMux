package com.tterrag.chatmux.util;

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
import com.tterrag.chatmux.links.ChatSource;

import lombok.Getter;

@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public abstract class Service<I, O> {
    
    public static final DiscordService DISCORD = new DiscordService();
    public static final FactorioService FACTORIO = new FactorioService();
    public static final MixerService MIXER = new MixerService();
    public static final TwitchService TWITCH = new TwitchService();

    private static final Map<String, Service<?, ?>> types = new HashMap<>();
    
    @Getter
    private final String name;
    
    protected Service(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        types.put(this.name, this);
    }
    
    public abstract ChatSource<I, O> getSource();
    
    @Override
    public String toString() {
        return name;
    }

    public static final Service<?, ?> byName(String name) {
        return types.get(name.toLowerCase(Locale.ROOT));
    }

    public static class Conv implements Converter<Service<?, ?>, String> {

        @Override
        public Service<?, ?> convertToField(String value) {
            return byName(value);
        }

        @Override
        public String convertFromField(Service<?, ?> value) {
            return value.name;
        }
    }
}

class Serializer extends JsonSerializer<Service<?, ?>> {

    @Override
    public void serialize(Service<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());
    }
}

class Deserializer extends JsonDeserializer<Service<?, ?>> {

    @Override
    public Service<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Service.byName(p.getValueAsString());
    }
}
