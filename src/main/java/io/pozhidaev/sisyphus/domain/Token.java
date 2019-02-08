package io.pozhidaev.sisyphus.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class Token {

    @JsonProperty("token")
    private String literal;


    public void setLiteral(String literal) {
        this.literal = literal;
    }
}
