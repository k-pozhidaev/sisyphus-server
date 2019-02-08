package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TokenFilterTest {

    @MockBean
    Token authToken;


    @Test
    public void filter() {

        Mockito.when(authToken.getLiteral()).thenReturn("test");

        final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/json/token").header("X-Token", "test").build());

        final WebFilterChain webFilterChain = Mockito.mock(WebFilterChain.class);
//        Mockito.when(webFilterChain.filter(exchange))
        final TokenFilter tokenFilter = new TokenFilter();
        final Token token = new Token();
        token.setLiteral("test");
        tokenFilter.setAuthToken(token);
        tokenFilter.filter(exchange, webFilterChain);
    }
}