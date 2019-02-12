package io.pozhidaev.sisyphus.configurations;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class LocalStorageConfigurationTest {


    @Test
    public void channelFunction() throws IOException {
        final Path tempFile = Files.createTempFile("channelFunction", "test");
        final LocalStorageConfiguration localStorageConfiguration = new LocalStorageConfiguration();
        final Mono<AsynchronousFileChannel> channelMono = localStorageConfiguration.channelFunction().apply(tempFile);
        channelMono
            .doOnError(throwable -> fail())
            .doOnSuccess(asynchronousFileChannel -> assertTrue(asynchronousFileChannel.isOpen()))
            .subscribe()
        ;
    }


    @Test
    public void channelFunction_exception() {
        final Path tempFile = Paths.get("not_exists.bkp");
        final LocalStorageConfiguration localStorageConfiguration = new LocalStorageConfiguration();
        final Mono<AsynchronousFileChannel> channelMono = localStorageConfiguration.channelFunction().apply(tempFile);
        channelMono
            .doOnError(throwable -> assertEquals("File open operation fault", throwable.getMessage()))
            .doOnSuccess(asynchronousFileChannel -> fail())
            .subscribe();

    }
}