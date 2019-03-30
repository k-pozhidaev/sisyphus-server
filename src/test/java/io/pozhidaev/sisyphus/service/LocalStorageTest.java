package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.File;
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

    @Test(expected = NullPointerException.class)
    public void putObject_nullPointer_1() {
        localStorage.putObject(null, Flux.just(stringBuffer("foo")));
    }

    @Test(expected = NullPointerException.class)
    public void putObject_nullPointer_2() {
        localStorage.putObject(1L, null);
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

        final File file = File.builder().id(1L).build();
        localStorage.setFileDirectory(fileDir);
        localStorage.createFile(file).subscribe(path ->
            assertTrue(Files.exists(Paths.get(fileDir.toString(), path.getId().toString())))
        );
    }

    @Test(expected = NullPointerException.class)
    public void createFile_nullPointer() {
        localStorage.createFile(null);
    }

    @Test
    public void createFile_exception() {

        final Path fileDir = Paths.get("fileDirectoryThatsNotExists");
        localStorage.setFileDirectory(fileDir);

        final File file = File.builder().id(1L).build();
        localStorage.createFile(file)
            .doOnError(throwable -> assertEquals(throwable.getMessage(), "File creation failed: 1"))
            .subscribe(integer -> fail());
    }


    @Test
    public void writeChunk() throws IOException {

        final Path filePath = filePathToWrite("writeChunk_test");
        localStorage.setFileDirectory(filePath.getParent());

        localStorage.setChannelFunction(path -> Mono.fromSupplier(() -> {
            try {
                return AsynchronousFileChannel.open(path, WRITE);
            } catch (IOException e) {
                throw new RuntimeException("File open operation fault", e);
            }
        }));

        localStorage
            .writeChunk(1L, Flux.just(stringBuffer("foo"), stringBuffer("baz")), 0L)
            .doOnSuccess(integer -> assertEquals(integer, Integer.valueOf(6)))
            .thenMany(
                localStorage.writeChunk(1L, Flux.just(stringBuffer("bar")), 6L)
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

    @Test(expected = NullPointerException.class)
    public void writeChunk_nullPointer_1() {
        localStorage
            .writeChunk(null, Flux.just(stringBuffer("foo")), 0).block();
    }

    @Test(expected = NullPointerException.class)
    public void writeChunk_nullPointer_2() {
        localStorage
            .writeChunk(1L, null, 0).block();

    }

    @Test
    public void closeChannel() throws IOException {
        final AsynchronousFileChannel channel = Mockito.mock(AsynchronousFileChannel.class);
        Mockito.doNothing().when(channel).close();
        final LocalStorage localStorage = new LocalStorage();
        localStorage.closeChannel(channel);
    }

    @Test(expected = NullPointerException.class)
    public void closeChannel_nullPointer() {
        localStorage.closeChannel(null);
    }

    @Test(expected = RuntimeException.class)
    public void closeChannel_exception() throws IOException {
        final AsynchronousFileChannel channel = Mockito.mock(AsynchronousFileChannel.class);
        Mockito.doThrow(IOException.class).when(channel).close();
        final LocalStorage localStorage = new LocalStorage();
        localStorage.closeChannel(channel);
    }

    @Test(expected = NullPointerException.class)
    public void flushBufferToFile_nullPointer_1() {
        localStorage.flushBufferToFile(null, 1L);
    }

    @Test(expected = NullPointerException.class)
    public void flushBufferToFile_nullPointer_2() {
        localStorage.flushBufferToFile(stringBuffer("fii"), null);
    }

    private Path filePathToWrite(final String testName) throws IOException {
        final Path fileDir = Files.createTempDirectory(testName).toAbsolutePath();
        final Path filePath = Paths.get(fileDir.toString(), "1");
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