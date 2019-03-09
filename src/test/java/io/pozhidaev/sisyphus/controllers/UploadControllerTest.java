package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.domain.File;
import io.pozhidaev.sisyphus.repository.FileRepository;
import io.pozhidaev.sisyphus.service.UploadService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RunWith(SpringRunner.class)
@WebFluxTest(UploadController.class)
public class UploadControllerTest {

    @Autowired
    private WebTestClient webClient;
    @MockBean
    UploadService uploadService;
    @MockBean
    FileRepository filesRepository;
    @MockBean
    ServerHttpRequest request;

    @Test
    public void getFilesList() {
        final List<File> files = Collections.singletonList(File.builder().id(1L).build());
        Mockito
            .when(filesRepository.findAll(PageRequest.of(0, 50)))
            .thenReturn(new PageImpl<>(files));
        webClient.get().uri("/upload").exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(File.class);
    }

    @Test
    public void getFileInfo() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Optional.of(File.builder().id(1L).build()));
        webClient.get().uri("/upload/1").exchange()
            .expectStatus()
            .isOk()
            .expectBody(File.class);
    }


    @Test(expected = NullPointerException.class)
    public void getFileInfo_notNull() {
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.getFileInfo(null);
    }

    @Test
    public void getFileInfo_notFound(){
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Optional.empty());
        webClient.get().uri("/upload/1").exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    public void uploadStart() {
        Mockito
            .when(uploadService.parseMetadata("testMetadata"))
            .thenReturn(new HashMap<String, String>(){{
                put("filename", "metadata");
            }});

        final File build = File.builder()
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .build();

        Mockito
            .when(uploadService.createUpload(build))
            .thenReturn(Mono.just(File.builder()
                .id(1L)
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .build()));

        webClient
            .post()
            .uri("/upload")
            .header("Upload-Length", "100")
            .header("Upload-Metadata", "testMetadata")
            .header("Mime-Type", "plain/text")
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location");
    }

    @Test
    public void uploadStart_doOnError() {
        Mockito
            .when(uploadService.parseMetadata("testMetadata"))
            .thenReturn(new HashMap<String, String>(){{
                put("filename", "metadata");
            }});

        final File metadata = File.builder()
                .id(1L)
                .mimeType("plain/text")
                .contentLength(100L)
                .originalName("metadata")
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .build();
        Mockito
            .when(uploadService.createUpload(metadata))
            .thenReturn(Mono.error(new Exception()));

        webClient
            .post()
            .uri("/upload")
            .header("Upload-Length", "100")
            .header("Upload-Metadata", "testMetadata")
            .header("Mime-Type", "plain/text")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test(expected = NullPointerException.class)
    public void uploadStart_nullPointer_1() {
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.uploadStart(15L, "", "", null, null);
    }

    @Test(expected = NullPointerException.class)
    public void uploadStart_nullPointer_2() {

        final UriComponentsBuilder mock = Mockito.mock(UriComponentsBuilder.class);
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.uploadStart(15L, "", "", mock, null);

    }



    @Test
    public void uploadProcess() {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DefaultDataBuffer dataBuffer =
            factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
        Flux<DataBuffer> body = Flux.just(dataBuffer);

        Mockito.when(request.getBody())
            .thenReturn(body);
        Mockito.when(request.getHeaders())
            .thenReturn(new HttpHeaders(){{
                put("test", Collections.singletonList("test"));
            }});
        Mockito
            .when(uploadService.uploadChunkAndGetUpdatedOffset(1L, body, 0))
            .thenReturn(Mono.just(File.builder().contentOffset(3L).build()));


        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.uploadProcess(1L, request, 0, 0)
            .subscribe(v -> {
                assertEquals(v.getStatusCode(), NO_CONTENT);
                assertEquals(Objects.requireNonNull(v.getHeaders().get("Upload-Offset")).get(0), "3");
            });
    }

    @Test(expected = NullPointerException.class)
    public void uploadProcess_nullPointer_1() {
        final ServerHttpRequest mock = Mockito.mock(ServerHttpRequest.class);
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.uploadProcess(null, mock, 0, 0);
    }

    @Test(expected = NullPointerException.class)
    public void uploadProcess_nullPointer_2() {
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.uploadProcess(1L, null, 0, 0);
    }

    @Test
    public void header() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Optional.of(File.builder().id(1L)
                .contentLength(100L)
                .contentOffset(0L).build()));
        webClient
            .head()
            .uri("/upload/1")
            .exchange()
            .expectStatus().isNoContent()
            .expectHeader().exists("Upload-Length")
            .expectHeader().exists("Upload-Offset")
            .expectHeader().exists("Cache-Control")
            .expectHeader().exists("Location")
        ;
    }

    @Test
    public void header_notFound() {
        Mockito
            .when(filesRepository.findById(1L))
            .thenReturn(Optional.empty());
        webClient
            .head()
            .uri("/upload/1")
            .exchange()
            .expectStatus().isNotFound()
        ;
    }

    @Test(expected = NullPointerException.class)
    public void header_nullPointer() {
        final UploadController uploadController = new UploadController(uploadService, filesRepository);
        uploadController.header(null);
    }

    @Test
    public void processOptions() {
        webClient
            .options()
            .uri("/upload")
            .exchange()
            .expectStatus().isNoContent()
            .expectHeader().exists("Tus-Version")
            .expectHeader().exists("Tus-Resumable")
            .expectHeader().exists("Access-Control-Expose-Headers")
            .expectHeader().exists("Tus-Extension")
            .expectHeader().exists("Access-Control-Allow-Methods")
        ;

    }
}