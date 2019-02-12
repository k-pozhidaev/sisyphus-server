package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TokenFilterTest {

    @MockBean
    Token authToken;


    @Test
    public void filter() {

        Mockito.when(authToken.getLiteral()).thenReturn("test");

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("X-Token", Collections.singletonList("test"));

        final ServerHttpRequest httpRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(httpRequest.getHeaders()).thenReturn(httpHeaders);

        final ServerWebExchange exchange = Mockito.mock(ServerWebExchange.class);
        Mockito.when(exchange.getRequest()).thenReturn(httpRequest);

        final WebFilterChain webFilterChain = Mockito.mock(WebFilterChain.class);

        final TokenFilter tokenFilter = new TokenFilter();

        Mockito.when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        tokenFilter.setAuthToken(authToken);
        tokenFilter
            .filter(exchange, webFilterChain)
            .doOnError(throwable -> fail())
            .doOnSuccess(aVoid -> assertTrue(true))
            .block();
    }
    @Test(expected = RuntimeException.class)
    public void filter_fail() {

        Mockito.when(authToken.getLiteral()).thenReturn("test1");

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("X-Token", Collections.singletonList("test"));

        final ServerHttpRequest httpRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(httpRequest.getHeaders()).thenReturn(httpHeaders);

        final ServerWebExchange exchange = Mockito.mock(ServerWebExchange.class);
        Mockito.when(exchange.getRequest()).thenReturn(httpRequest);

        final WebFilterChain webFilterChain = Mockito.mock(WebFilterChain.class);

        final TokenFilter tokenFilter = new TokenFilter();

        Mockito.when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        tokenFilter.setAuthToken(authToken);
        tokenFilter
            .filter(exchange, webFilterChain)
            .doOnError(throwable -> assertTrue(true))
            .doOnSuccess(aVoid -> fail())
            .block();
    }
}