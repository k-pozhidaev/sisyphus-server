package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.File;
import io.pozhidaev.sisyphus.repository.FileRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RunWith(SpringRunner.class)
public class UploadServiceTest {


    @MockBean
    FileStorage fileStorage;

    @MockBean
    FileRepository fileRepository;

    private UploadService uploadService;

    @Before
    public void before() {
        uploadService = new UploadService(fileStorage, fileRepository);
    }


    @Test
    public void uploadChunkAndGetUpdatedOffset() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DefaultDataBuffer dataBuffer =
                factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        Flux<DataBuffer> body = Flux.just(dataBuffer);
        final long id = 1L;
        final File file = File
                .builder()
                .id(id)
                .originalName("test")
                .contentLength(55L)
                .mimeType("test")
                .lastUploadedChunkNumber(0L)
                .contentOffset(0L)
                .build();

        Mockito.when(fileStorage.putObject(id, body)).thenReturn(Mono.just(55));
        Mockito.when(fileRepository.getOne(id)).thenReturn(file);

        final Mono<Long> longMono = uploadService.uploadChunkAndGetUpdatedOffset(id, body);
        longMono.subscribe(v -> Assert.assertEquals(v, new Long(55)));
    }

    @Test
    public void parseMetadata() {
        final String test1 = "Test";
        final String test = Base64.getEncoder().encodeToString(test1.getBytes());
        final String test21 = "test2";
        final String test2 = Base64.getEncoder().encodeToString(test21.getBytes());
        final Map<String, String> stringStringMap = uploadService.parseMetadata("test " + test + ",test2 " + test2);
        Assert.assertEquals(test1, stringStringMap.get("test"));
        Assert.assertEquals(test21, stringStringMap.get("test2"));
    }

    @Test(expected = NullPointerException.class)
    public void parseMetadata_null() {
        final Map<String, String> stringStringMap = uploadService.parseMetadata(null);
        Assert.assertNull(stringStringMap);

    }

    @Test(expected = RuntimeException.class)
    public void parseMetadata_exception() {
        uploadService.parseMetadata("test test, test1 test");
    }

    @Test(expected = RuntimeException.class)
    public void parseMetadata_exception1() {

        final String test1 = "Test";
        final String test = Base64.getEncoder().encodeToString(test1.getBytes());
        uploadService.parseMetadata("test " + test + ", test1 " + test);
    }
}