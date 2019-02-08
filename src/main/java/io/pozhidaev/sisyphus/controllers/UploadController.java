package io.pozhidaev.sisyphus.controllers;

import io.pozhidaev.sisyphus.domain.File;
import io.pozhidaev.sisyphus.repository.FileRepository;
import io.pozhidaev.sisyphus.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

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
    ){
        return Mono
                .fromSupplier(() -> ResponseEntity.ok(filesRepository.findAll(PageRequest.of(page, 50))));
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getFileInfo(@PathVariable("id") Long id){
        return Mono
            .fromSupplier(() ->
                filesRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build())
            );
    }

    @PostMapping
    public Mono<ResponseEntity<?>> uploadStart(
        @RequestHeader(name = "Upload-Length") final Long fileSize,
        @RequestHeader(name = "Upload-Metadata") final String metadata,
        @RequestHeader(name = "Mime-Type") final String mimeType,
        final UriComponentsBuilder uriComponentsBuilder,
        ServerHttpRequest request
    ) {
        request.getHeaders().forEach((k,v) -> log.debug("headers: {} {}", k, v));


        final Map<String, String> parsedMetadata = uploadService.parseMetadata(metadata);

        final File file = filesRepository.save(File.builder()
            .mimeType(mimeType)
            .contentLength(fileSize)
            .originalName(parsedMetadata.getOrDefault("filename", "FILE NAME NOT EXISTS"))
            .contentOffset(0L)
            .lastUploadedChunkNumber(0L)
            .build());

        return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
            .header("Access-Control-Expose-Headers", "Location, Tus-Resumable")
            .header("Location", uriComponentsBuilder.path("upload/" + file.getId().toString())
                .build()
                .toString()
            )
            .header("Tus-Resumable", "1.0.0")
            .build());
    }

    @RequestMapping(
        method = {RequestMethod.POST, RequestMethod.PATCH,},
        value = {"/{id}"},
        consumes = {"application/offset+octet-stream"}
        )
    public Mono<ResponseEntity<?>> uploadProcess(
        @PathVariable("id") final Long id,
        ServerHttpRequest request
    )  {
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
            .orElseGet(() -> ResponseEntity.badRequest().build()));
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
