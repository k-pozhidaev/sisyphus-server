package io.pozhidaev.sisyphus.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class Token {

    @JsonProperty("token")
    private String literal;


}
