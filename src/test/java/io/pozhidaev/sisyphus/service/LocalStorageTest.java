package io.pozhidaev.sisyphus.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class LocalStorageTest {

    private LocalStorage localStorage;

    @Before
    public void setUp() {
        this.localStorage = new LocalStorage();
    }

    @Test
    public void putObject() throws IOException {

        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DefaultDataBuffer dataBuffer =
            factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        Flux<DataBuffer> body = Flux.just(dataBuffer);

        final Path fileDir = Files.createTempDirectory("fileDirectory").toAbsolutePath();
        localStorage.setFileDirectory(fileDir);
        localStorage.putObject(1L, body).subscribe(v -> assertEquals(v, Integer.valueOf(3)));
    }

    @Test(expected = RuntimeException.class)
    public void putObject_exception() {

        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DefaultDataBuffer dataBuffer =
            factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        Flux<DataBuffer> body = Flux.just(dataBuffer);

        final Path fileDir = Paths.get("fileDirectoryThatsNotExists");
        localStorage.setFileDirectory(fileDir);
        localStorage.putObject(1L, body).subscribe(v -> assertEquals(v, Integer.valueOf(3)));
    }
}