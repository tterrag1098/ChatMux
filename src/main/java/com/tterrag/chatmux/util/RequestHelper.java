package com.tterrag.chatmux.util;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.reactivestreams.Publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
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
                                .wiretap();
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
    
    private <T> Mono<T> handleResponse(HttpClientResponse resp, ByteBufMono body, Class<? extends T> type) {
        int response = resp.status().code();
        
        if (response / 100 != 2) {
            return body.asString().flatMap(err -> Mono.error(new IOException(resp.method().asciiName() + " " + resp.uri() + " failed (" + resp.status().code() + ") " + err)));
        }
        
        return body.asInputStream().map(is -> runUnchecked(() -> mapper.readValue(is, type)));
    }
    
    public <T> Mono<T> get(String endpoint, Class<? extends T> type) {
        return request(endpoint, HttpMethod.GET).<T>responseSingle((r, buf) -> handleResponse(r, buf, type)).doOnError(t -> log.error("Error during GET", t));
    }
    
    private Publisher<? extends ByteBuf> encodePayload(Object payload) {
        return Mono.just(payload)
                .map(p -> p instanceof String ? ((String) p).replaceAll("\\r?\\n", "\\\\n") : runUnchecked(() -> mapper.writeValueAsString(p)))
                .map(json -> Unpooled.wrappedBuffer(json.getBytes(Charsets.UTF_8)));
    }
    
    public ResponseReceiver<?> post(String endpoint, Object payload) {
        return request(endpoint, HttpMethod.POST).send(encodePayload(payload));
    }
    
    public <T> Mono<T> post(String endpoint, Object payload, Class<? extends T> type) {
        return post(endpoint, payload).<T>responseSingle((r, buf) -> handleResponse(r, buf, type)).doOnError(t -> log.error("Error during POST", t));
    }

    protected Disposable postVoid(String endpoint, Object payload) {
        return post(endpoint, payload).response().doOnError(Throwable::printStackTrace).subscribe();
    }
    
    public Disposable delete(String endpoint) {
        return request(endpoint, HttpMethod.DELETE).response().doOnError(Throwable::printStackTrace).subscribe();
    }
    
    public Disposable put(String endpoint) {
        return request(endpoint, HttpMethod.PUT).response().doOnNext(System.out::println).doOnError(Throwable::printStackTrace).subscribe();
    }
    
    public Disposable patch(String endpoint, Object payload) {
        return request(endpoint, HttpMethod.PATCH)
                .send(encodePayload(payload))
                .response()
                .subscribe();
    }
}
