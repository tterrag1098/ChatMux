package com.tterrag.chatmux.bridge;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

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

import lombok.Getter;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@JsonSerialize(using = Serializer.class)
@JsonDeserialize(using = Deserializer.class)
public abstract class ChatService<I, O> implements ExtensionPoint {

    private static final Map<String, ChatService<?, ?>> types = new HashMap<>();
    
    private ChatSource<I, O> source;
    
    @Getter
    @NonNull
    private final String name;
    
    protected ChatService(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        types.put(this.name, this);
    }
    
    public void initialize() {
        source = createSource();
    }
    
    public final ChatSource<I, O> getSource() {
        final ChatSource<I, O> source = this.source;
        if (source == null) {
            throw new IllegalStateException("Source not created");
        }
        return source;
    }
    
    protected abstract ChatSource<I, O> createSource();
    
    public Mono<Void> runInterface() {
        throw new UnsupportedOperationException("Service '" + name + "' cannot be used as main");
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Nullable
    public static final ChatService<?, ?> byName(@Nullable String name) {
        return name == null ? null : types.get(name.toLowerCase(Locale.ROOT));
    }

    public static class Conv implements Converter<ChatService<?, ?>, String> {

        @Override
        public @Nullable ChatService<?, ?> convertToField(@Nullable String value) {
            return byName(value);
        }

        @Override
        public String convertFromField(@SuppressWarnings("null") ChatService<?, ?> value) {
            return value.name;
        }
    }
}

class Serializer extends JsonSerializer<ChatService<?, ?>> {

    @SuppressWarnings("null")
    @Override
    public void serialize(ChatService<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getName());
    }
}

class Deserializer extends JsonDeserializer<ChatService<?, ?>> {

    @SuppressWarnings("null")
    @Override
    public @Nullable ChatService<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return ChatService.byName(p.getValueAsString());
    }
}
