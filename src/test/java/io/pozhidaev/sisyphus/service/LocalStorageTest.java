package io.pozhidaev.sisyphus.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
        Flux<DataBuffer> body = Flux.just(stringBuffer("foo"));
        final Path fileDir = Files.createTempDirectory("fileDirectory").toAbsolutePath();
        localStorage.setFileDirectory(fileDir);
        localStorage.putObject(1L, body).subscribe(v -> assertEquals(v, Integer.valueOf(3)));
        assertTrue(Files.exists(Paths.get(fileDir.toString(), "1")));
    }

    @Test
    public void putObject_fileExists() throws IOException {
        Flux<DataBuffer> body = Flux.just(stringBuffer("foo"));
        final Path fileDir = Files.createTempDirectory("fileDirectory").toAbsolutePath();
        Files.createFile(Paths.get(fileDir.toString(), "1"));
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
    public void createFile() throws IOException {
        final Path fileDir = Files.createTempDirectory("fileDirectory").toAbsolutePath();
        localStorage.setFileDirectory(fileDir);
        localStorage.createFile(1L).subscribe(path ->
            assertTrue(Files.exists(path))
        );
    }

    @Test
    public void createFile_exception() {

        final Path fileDir = Paths.get("fileDirectoryThatsNotExists");
        localStorage.setFileDirectory(fileDir);
        localStorage.createFile(1L)
            .doOnError(throwable -> assertEquals(throwable.getMessage(), "File creation failed: 1"))
            .subscribe(integer -> fail());
    }


    @Test
    public void writeChunk() throws IOException {

        final Path filePath = filePathToWrite("writeChunk_test", "1");
        localStorage.setFileDirectory(filePath.getParent());

        localStorage.setChannelFunction(path -> Mono.fromSupplier(() -> {
            try {
                return AsynchronousFileChannel.open(path, WRITE);
            } catch (IOException e) {
                throw new RuntimeException("File open operation fault", e);
            }
        }));

        localStorage
            .writeChunk(1L, Flux.just(stringBuffer("foo"), stringBuffer("baz")), 0L, 6L)
            .doOnSuccess(integer -> assertEquals(integer, Integer.valueOf(6)))
            .thenMany(
                localStorage.writeChunk(1L, Flux.just(stringBuffer("bar")), 6L, 3L)
                    .doOnSuccess(integer -> assertEquals(integer, Integer.valueOf(3)))
            )
            .then()
            .doOnSuccess(aVoid -> {
                try {
                    Files.lines(filePath)
                        .findFirst()
                        .map(s -> {
                            assertEquals("foobazbar", s);
                            return 1;
                        })
                        .orElseGet(() -> {
                            fail();
                            return 1;
                        });
                } catch (IOException e) {
                    throw new RuntimeException("Read result error", e);
                }
            })
            .subscribe();
    }

    @Test
    public void writeChunk_bufferNotEqualsException() throws IOException {
        final Path filePath = filePathToWrite("writeChunk_bufferNotEqualsException", "1");
        localStorage.setFileDirectory(filePath.getParent());

        localStorage.setChannelFunction(path -> Mono.fromSupplier(() -> {
            try {
                return AsynchronousFileChannel.open(path, WRITE);
            } catch (IOException e) {
                throw new RuntimeException("File open operation fault", e);
            }
        }));

        localStorage
            .writeChunk(1L, Flux.just(stringBuffer("foo"), stringBuffer("baz")), 0L, 7L)
            .doOnError(throwable -> assertEquals(throwable.getMessage(), "Buffer not equals size"))
            .subscribe(integer -> fail());
    }

    @Test
    public void closeChannel() throws IOException {
        final AsynchronousFileChannel channel = Mockito.mock(AsynchronousFileChannel.class);
        Mockito.doNothing().when(channel).close();
        final LocalStorage localStorage = new LocalStorage();
        localStorage.closeChannel(channel);
    }

    @Test(expected = RuntimeException.class)
    public void closeChannel_exception() throws IOException {
        final AsynchronousFileChannel channel = Mockito.mock(AsynchronousFileChannel.class);
        Mockito.doThrow(IOException.class).when(channel).close();
        final LocalStorage localStorage = new LocalStorage();
        localStorage.closeChannel(channel);
    }

    private Path filePathToWrite(final String testName, final String fileName) throws IOException {
        final Path fileDir = Files.createTempDirectory(testName).toAbsolutePath();
        final Path filePath = Paths.get(fileDir.toString(), fileName);
        if (Files.exists(filePath)) Files.delete(filePath);
        return Files.createFile(filePath).toAbsolutePath();
    }


    private DataBuffer stringBuffer(String value) {
        return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
    }

    private DataBuffer byteBuffer(byte[] value) {
        final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(true);
        DataBuffer buffer = bufferFactory.allocateBuffer(value.length);
        buffer.write(value);
        return buffer;
    }
}