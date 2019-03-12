package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.domain.File;
import io.pozhidaev.sisyphus.repository.FileRepository;
import io.pozhidaev.sisyphus.service.UploadService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

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
    public Mono<ResponseEntity<?>> getFileInfo(@NonNull @PathVariable("id") Long id) {
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
        @RequestHeader(name = "Mime-Type", defaultValue = "") final String mimeType,
        @NonNull final UriComponentsBuilder uriComponentsBuilder,
        @NonNull final ServerHttpRequest request
    ) {
        request.getHeaders().forEach((k, v) -> log.debug("headers: {} {}", k, v));

        final Map<String, String> parsedMetadata = uploadService.parseMetadata(metadata);

        final File file = File.builder()
                .mimeType(mimeType)
                .contentLength(fileSize)
                .originalName(parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"))
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .fingerprint(parsedMetadata.get("fingerprint"))
                .build();

        return uploadService
            .createUpload(file)
            .map(f -> Stream.concat(
                request.getPath().elements().stream().map(PathContainer.Element::value),
                Stream.of(f.getId().toString())
            ))
            .map(stringStream -> stringStream.filter(s -> !"/".equals(s)).toArray(String[]::new))
            .map(strings -> uriComponentsBuilder.pathSegment(strings).build().toUri())
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
            );
    }

    @RequestMapping(
        method = {RequestMethod.POST, RequestMethod.PATCH,},
        value = {"/{id}"},
        consumes = {"application/offset+octet-stream"}
    )
    public Mono<ResponseEntity<?>> uploadProcess(
        @NonNull @PathVariable("id") final Long id,
        @NonNull final ServerHttpRequest request,
        @RequestHeader(name = "Upload-Offset") final long offset,
        @RequestHeader(name = "Content-Length") final long length
    ) {
        request.getHeaders().forEach((k, v) -> log.debug("headers: {} {}", k, v));

        return
            uploadService
                .uploadChunkAndGetUpdatedOffset(
                    id,
                    request.getBody(),
                    offset,
                    length
                )
                .log()
                .map(e -> ResponseEntity
                    .status(NO_CONTENT)
                    .header("Access-Control-Expose-Headers", "Location, Tus-Resumable")
                    .header("Upload-Offset", Long.toString(e.getContentOffset()))
                    .header("Tus-Resumable", "1.0.0")
                    .build()
                );
    }


    @RequestMapping(method = RequestMethod.HEAD, value = "/{id}")
    public Mono<ResponseEntity<?>> header(@NonNull @PathVariable("id") final Long id) {
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
    public Mono<ResponseEntity> processOptions() {
        return Mono.just(ResponseEntity
            .status(NO_CONTENT)
            .header("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension")
            .header("Tus-Resumable", "1.0.0")
            .header("Tus-Version", "1.0.0,0.2.2,0.2.1")
            .header("Tus-Extension", "creation,expiration")
            .header("Access-Control-Allow-Methods", "GET,PUT,PATCH,POST,DELETE")
            .build());
    }


}
