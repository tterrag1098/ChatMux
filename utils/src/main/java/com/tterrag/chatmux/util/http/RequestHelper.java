package com.tterrag.chatmux.util.http;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.RequestSender;
import reactor.netty.http.client.HttpClient.ResponseReceiver;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.annotation.NonNull;

@Slf4j
public abstract class RequestHelper {
    
    protected final @NonNull ObjectMapper mapper;
    
    protected final @NonNull HttpClient client;
    
    protected RequestHelper(ObjectMapper mapper, String baseUrl) {
        this.mapper = mapper;
        this.client = HttpClient.create()
                                .baseUrl(baseUrl)
                                .headers(this::addHeaders)
                                .wiretap(true);
    }
    
    protected RequestSender request(String endpoint, HttpMethod method) {
        return client.request(method).uri(endpoint);
    }
    
    protected abstract void addHeaders(HttpHeaders headers);
    
    private <T> T runUnchecked(Callable<T> func) {
        try {
            return func.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected <T> Mono<T> handleResponse(HttpClientResponse resp, ByteBufMono body, JavaType type) {
        int response = resp.status().code();
        
        if (response / 100 != 2) {
            return body.asString().flatMap(err -> Mono.error(new IOException(resp.method().asciiName() + " " + resp.uri() + " failed (" + resp.status().code() + ") " + err)));
        }
        
        return body.asInputStream().map(is -> runUnchecked(() -> mapper.readValue(is, type)));
    }
    
    public <T> Mono<T> get(String endpoint, Class<? extends T> type) {
        return get(endpoint, TypeFactory.defaultInstance().constructType(type));
    }
    
    public <T> Mono<T> get(String endpoint, TypeReference<? extends T> type) {
        return get(endpoint, TypeFactory.defaultInstance().constructType(type));
    }
    
    public <T> Mono<T> get(String endpoint, JavaType type) {
        return request(endpoint, HttpMethod.GET).<T>responseSingle((r, buf) -> handleResponse(r, buf, type)).doOnError(requestError(HttpMethod.GET, endpoint));
    }
    
    protected final Publisher<? extends ByteBuf> encodePayload(Object payload) {
        return Mono.just(payload)
                .map(p -> p instanceof String ? ((String) p).replaceAll("\\r?\\n", "\\\\n") : runUnchecked(() -> mapper.writeValueAsString(p)))
                .map(json -> Unpooled.wrappedBuffer(json.getBytes(CharsetUtil.UTF_8)));
    }
    
    public ResponseReceiver<?> post(String endpoint, Object payload) {
        return request(endpoint, HttpMethod.POST).send(encodePayload(payload));
    }
    
    public <T> Mono<T> post(String endpoint, Object payload, Class<? extends T> type) {
        return post(endpoint, payload, TypeFactory.defaultInstance().constructType(type));
    }
    
    public <T> Mono<T> post(String endpoint, Object payload, TypeReference<? extends T> type) {
        return post(endpoint, payload, TypeFactory.defaultInstance().constructType(type));
    }
    
    public <T> Mono<T> post(String endpoint, Object payload, JavaType type) {
        return post(endpoint, payload).<T>responseSingle((r, buf) -> handleResponse(r, buf, type)).doOnError(requestError(HttpMethod.POST, endpoint));
    }

    protected Mono<Void> postVoid(String endpoint, Object payload) {
        return post(endpoint, payload).response().doOnError(requestError(HttpMethod.POST, endpoint)).then();
    }
    
    public Mono<Void> delete(String endpoint) {
        return request(endpoint, HttpMethod.DELETE).response().doOnError(requestError(HttpMethod.DELETE, endpoint)).then();
    }
    
    public Mono<Void> put(String endpoint) {
        return request(endpoint, HttpMethod.PUT).response().doOnError(requestError(HttpMethod.PUT, endpoint)).then();
    }
    
    public Mono<Void> patch(String endpoint, Object payload) {
        return request(endpoint, HttpMethod.PATCH)
                .send(encodePayload(payload))
                .response()
                .doOnError(requestError(HttpMethod.PATCH, endpoint))
                .then();
    }
    
    private Consumer<Throwable> requestError(HttpMethod method, String endpoint) {
        return t -> log.error("Error during " + method.name() + " to " + endpoint, t);
    }
}
