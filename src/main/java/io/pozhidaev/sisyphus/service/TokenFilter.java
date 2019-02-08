package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Component
public class TokenFilter implements WebFilter {

    private Token authToken;

    @Autowired
    public void setAuthToken(Token authToken) {
        this.authToken = authToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
                             WebFilterChain webFilterChain) {
        return serverWebExchange
                .getRequest()
                .getHeaders()
                .getOrDefault("X-Token", Collections.singletonList("wrong token"))
                .stream()
                .filter(e -> e.equals(authToken.getLiteral()))
                .findFirst()
                .map(e -> webFilterChain.filter(serverWebExchange))
                .orElseGet(() -> Mono.error(new Exception("Invalid token")));
    }
}
