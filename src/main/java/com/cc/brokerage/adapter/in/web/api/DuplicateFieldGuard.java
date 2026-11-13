package com.cc.brokerage.adapter.in.web.api;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Rejects a JSON body that repeats a property, before it is bound. Jackson's own duplicate detection only
 * reports the problem in the text of its message, so it is checked here on the token stream instead.
 */
@RestControllerAdvice
public class DuplicateFieldGuard extends RequestBodyAdviceAdapter {

    private static final JsonFactory JSON = new JsonFactory();

    @Override
    public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage input,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        byte[] body = input.getBody().readAllBytes();
        rejectDuplicates(body);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return input.getHeaders();
            }
        };
    }

    static void rejectDuplicates(byte[] body) {
        Deque<Set<String>> objects = new ArrayDeque<>();
        try (JsonParser parser = JSON.createParser(ObjectReadContext.empty(), body)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                switch (token) {
                    case START_OBJECT -> objects.push(new HashSet<>());
                    case END_OBJECT -> objects.pop();
                    case PROPERTY_NAME -> {
                        if (!objects.peek().add(parser.currentName())) {
                            throw new DuplicateFieldException(parser.currentName());
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (JacksonException malformed) {
            // A malformed document is left to the message converter, which reports it as unreadable.
        }
    }
}
