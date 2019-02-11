package io.pozhidaev.sisyphus.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.WRITE;
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
        localStorage.putObject(1L, body)
            .subscribe(v -> assertEquals(v, Integer.valueOf(3)));
    }

    @Test
    public void writeChunk() throws IOException {

        if (Files.exists(Paths.get("1"))) Files.delete(Paths.get("1"));
        Files.createFile(Paths.get(Objects.toString(1L))).toAbsolutePath();
        localStorage.setChannelFunction(path -> Mono.fromSupplier(() -> {
            try {
                return AsynchronousFileChannel.open(path, WRITE);
            } catch (IOException e) {
                throw new RuntimeException("File open operation fault");
            }
        }));
        localStorage
            .writeChunk(1L, Flux.just(stringBuffer("foo"), stringBuffer("baz")), 0L, 6L)
            .subscribe(integer -> assertEquals(integer, Integer.valueOf(3)));
        localStorage.
            writeChunk(1L, Flux.just(stringBuffer("bar")), 6L, 3L)
            .subscribe(integer -> assertEquals(integer, Integer.valueOf(3)));


    }

    @Test
    public void name() {

    }


    protected DataBuffer stringBuffer(String value) {
        return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
    }

    protected DataBuffer byteBuffer(byte[] value) {
        final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(true);
        DataBuffer buffer = bufferFactory.allocateBuffer(value.length);
        buffer.write(value);
        return buffer;
    }
}