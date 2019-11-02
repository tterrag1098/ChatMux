package com.tterrag.chatmux.mixer.method;

import java.util.Arrays;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class MixerMethod {
    
    private static int idCounter = 0;
    
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum MethodType {
        AUTH("auth", 3, 1),
        MESSAGE("msg", 1, 0),
        PURGE("purge", 1, 0),
        DELETE_MESSAGE("deleteMessage", 1, 0);
        ;
        
        public final String id;
        public final int requiredArgs, optionalArgs;
        
        @Override
        @JsonValue
        public String toString() {
            return id;
        }
    }
    
    @JsonProperty
    private final String type = "method";
    @JsonProperty
    private final MethodType method;
    @JsonProperty("arguments")
    private final Object[] args;
    @Getter
    @JsonProperty
    private final int id;
    
    public MixerMethod(MethodType method, Object... args) {
        this.method = method;
        Preconditions.checkArgument(args.length >= method.requiredArgs, "Must supply at least %d arguments for type %s", method.requiredArgs, method);
        Preconditions.checkArgument(args.length <= method.requiredArgs + method.optionalArgs, "Cannot supply more than %d arguments for type %s", method.requiredArgs + method.optionalArgs, method);
        this.args = Arrays.copyOf(args, args.length);
        this.id = idCounter++;
    }

    public MixerMethod saveId(BiConsumer<Integer, MethodType> consumer) {
        consumer.accept(this.id, this.method);
        return this;
    }
}
