package com.tterrag.chatmux.bridge;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.pf4j.ExtensionPoint;

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
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.bridge.ChatSource;
import com.tterrag.chatmux.api.command.CommandHandler;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.links.LinkManager;

import lombok.Getter;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public abstract class AbstractChatService<M extends ChatMessage<M>, S extends ChatSource<M>> implements ChatService<M>, ExtensionPoint {

    private static final Map<String, AbstractChatService<?, ?>> types = new HashMap<>();
    
    private S source;
    
    @Getter
    @NonNull
    private final String name;
    
    protected AbstractChatService(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        types.put(this.name, this);
    }
    
    public void initialize() {
        source = createSource();
    }
    
    public final S getSource() {
        final S source = this.source;
        if (source == null) {
            throw new IllegalStateException("Source not created");
        }   
        return source;
    }
    
    protected abstract S createSource();
    
    public abstract @Nullable ServiceConfig<?> getConfig();
    
    public Mono<CommandHandler> getInterface(LinkManager manager) {
        throw new UnsupportedOperationException("Service '" + name + "' cannot be used as main");
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Nullable
    public static final AbstractChatService<?, ?> byName(@Nullable String name) {
        return name == null ? null : types.get(name.toLowerCase(Locale.ROOT));
    }

    public static class Conv implements Converter<ChatService<?>, String> {

        @Override
        public @Nullable ChatService<?> convertToField(@Nullable String value) {
            return byName(value);
        }

        @Override
        public String convertFromField(@SuppressWarnings("null") ChatService<?> value) {
            return value.getName();
        }
    }
}

class Serializer extends JsonSerializer<ChatService<?>> {

    @SuppressWarnings("null")
    @Override
    public void serialize(ChatService<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());
    }
}

class Deserializer extends JsonDeserializer<ChatService<?>> {

    @SuppressWarnings("null")
    @Override
    public @Nullable ChatService<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return AbstractChatService.byName(p.getValueAsString());
    }
}
