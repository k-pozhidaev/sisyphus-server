package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.domain.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("api/v1/json/token")
public class TokenController {

    private final Token authToken;

    @Autowired
    public TokenController(Token authToken) {
        this.authToken = authToken;
    }

    @GetMapping
    public Mono<Token> get(){
        return Mono.just(authToken);
    }

    @PostMapping
    public Mono<Token> update(@RequestBody Token token){
        authToken.setLiteral(token.getLiteral());
        return Mono.just(token);
    }

}
