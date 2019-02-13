package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.repository.FileRepository;
import io.pozhidaev.sisyphus.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;
    private final FileRepository filesRepository;


    @Autowired
    public UploadController(
        final UploadService uploadService,
        final FileRepository filesRepository
    ) {
        this.uploadService = uploadService;
        this.filesRepository = filesRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<?>> getFilesList(
        @RequestParam(name = "page", defaultValue = "0") int page
    ) {
        return Mono
            .fromSupplier(() -> ResponseEntity.ok(filesRepository.findAll(PageRequest.of(page, 50))));
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getFileInfo(@PathVariable("id") Long id) {
        return Mono
            .fromSupplier(() ->
                filesRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build())
            );
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> uploadStart(
        @RequestHeader(name = "Upload-Length") final Long fileSize,
        @RequestHeader(name = "Upload-Metadata") final String metadata,
        @RequestHeader(name = "Mime-Type") final String mimeType,
        final UriComponentsBuilder uriComponentsBuilder,
        ServerHttpRequest request
    ) {
        request.getHeaders().forEach((k, v) -> log.debug("headers: {} {}", k, v));

        final Map<String, String> parsedMetadata = uploadService.parseMetadata(metadata);
        //TODO hardcode is not acceptable!

        return uploadService
            .createUpload(
                fileSize,
                parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"),
                mimeType
            )
            .map(file -> uriComponentsBuilder.path("upload/" + file.getId().toString()).build().toUri())
            .map(s -> ResponseEntity
                .created(s)
                .header("Access-Control-Expose-Headers", "Location, Tus-Resumable")
                .header("Tus-Resumable", "1.0.0")
                .build()
            )
            .doOnError(throwable -> log.error("Error on file create", throwable))
            .onErrorReturn(ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .build()
            )
        ;
    }

    @RequestMapping(
        method = {RequestMethod.POST, RequestMethod.PATCH,},
        value = {"/{id}"},
        consumes = {"application/offset+octet-stream"}
    )
    public Mono<ResponseEntity<?>> uploadProcess(
        @PathVariable("id") final Long id,
        ServerHttpRequest request
    ) {
        return
            uploadService
                .uploadChunkAndGetUpdatedOffset(
                    id,
                    request.getBody()
                )
                .log()
                .map(e -> ResponseEntity
                    .status(NO_CONTENT)
                    .header("Access-Control-Expose-Headers", "Location, Tus-Resumable")
                    .header("Upload-Offset", Long.toString(e))
                    .header("Tus-Resumable", "1.0.0")
                    .build()
                );
    }


    @RequestMapping(method = RequestMethod.HEAD, value = "/{id}")
    public Mono<ResponseEntity<?>> header(@PathVariable("id") final Long id) {
        return Mono.just(filesRepository.findById(id).map(e ->
            ResponseEntity
                .status(NO_CONTENT)
                .headers(new HttpHeaders())
                .header("Location", e.getId().toString())
                .header("Cache-Control", "no-store")
                .header("Upload-Length", e.getContentLength().toString())
                .header("Upload-Offset", e.getContentOffset().toString())
                .build())
            .orElseGet(() -> ResponseEntity.notFound().build()));
    }


    @RequestMapping(method = RequestMethod.OPTIONS)
    public Mono<ResponseEntity<?>> processOptions() {
        return Mono.just(ResponseEntity
            .status(NO_CONTENT)
            .header("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension")
            .header("Tus-Resumable", "1.0.0")
            .header("Tus-Version", "1.0.0,0.2.2,0.2.1")
            .header("Tus-Extension", "creation,expiration")
            .header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE")
            .build());
    }


}
