package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.domain.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@WebFluxTest(TokenController.class)
public class TokenControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    Token authToken;


    @Test
    public void get_test()  {
        webClient.get().uri("/api/v1/json/token").exchange()
                .expectStatus()
                .isOk()
                .expectBody(Token.class);
    }

    @Test
    public void update_test() {
        final Token newToken = new Token();
        newToken.setLiteral("test");
        webClient.post()
                .uri("/api/v1/json/token")
                .body(Mono.just(newToken), Token.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Token.class)
        ;
    }
}