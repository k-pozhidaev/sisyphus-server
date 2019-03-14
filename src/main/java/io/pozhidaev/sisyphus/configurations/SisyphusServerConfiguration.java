package io.pozhidaev.sisyphus.configurations;

import io.pozhidaev.sisyphus.domain.Token;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "sisyphus-server")
@EnableConfigurationProperties(SisyphusServerConfiguration.class)
public class SisyphusServerConfiguration {

    private String fileDirectory;
    private String token;

    @Bean
    public NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(
        Environment environment,
        ServerProperties serverProperties
    ) {
        serverProperties.setMaxHttpHeaderSize(DataSize.ofMegabytes(10));
        return new NettyWebServerFactoryCustomizer(environment, serverProperties);
    }

    @Bean
    public Token authToken(){
        return Optional
                .ofNullable(token)
                .map(t -> {
                    Token token = new Token();
                    token.setLiteral(t);
                    return token;
                })
                .orElseThrow(() -> new RuntimeException("Server started without auth token."));
    }


    @Bean
    public Path fileDirectory() throws IOException {
        final Path writeDirectoryPath = Paths.get(fileDirectory);

        log.debug("Files path: {}, created: {}", writeDirectoryPath, Files.exists(writeDirectoryPath));
        if (!Files.exists(writeDirectoryPath)) {
            Files.createDirectories(writeDirectoryPath);
        }

        if (!Files.isWritable(writeDirectoryPath)) {
            throw new AccessDeniedException(fileDirectory);
        }
        return writeDirectoryPath;
    }

}
