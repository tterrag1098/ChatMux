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
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import lombok.Getter;

@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public final class ServiceType<I, O> {
    
    private static final Map<String, ServiceType<?, ?>> types = new HashMap<>();
    
    @Getter
    private final String name;
    
    private ServiceType(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        types.put(this.name, this);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public static final ServiceType<Dispatch, GatewayPayload<?>> DISCORD = new ServiceType<>("discord");
    public static final ServiceType<MixerEvent, MixerMethod> MIXER = new ServiceType<>("mixer");
    public static final ServiceType<IRCEvent, String> TWITCH = new ServiceType<>("twitch");
    
    public static final ServiceType<?, ?> byName(String name) {
        return types.get(name.toLowerCase(Locale.ROOT));
    }

    public static class Conv implements Converter<ServiceType<?, ?>, String> {

        @Override
        public ServiceType<?, ?> convertToField(String value) {
            return byName(value);
        }

        @Override
        public String convertFromField(ServiceType<?, ?> value) {
            return value.name;
        }
    }
}

class Serializer extends JsonSerializer<ServiceType<?, ?>> {

    @Override
    public void serialize(ServiceType<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());
    }
}

class Deserializer extends JsonDeserializer<ServiceType<?, ?>> {

    @Override
    public ServiceType<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return ServiceType.byName(p.getValueAsString());
    }
}
