package io.pozhidaev.sisyphus.service;


import io.pozhidaev.sisyphus.domain.File;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface FileStorage {
    Mono<File> createFile(final File id);
    Mono<Integer> putObject(final Long id, final Flux<DataBuffer> parts);
    Mono<Integer> writeChunk(final Long id, final Flux<DataBuffer> parts, final long offset, final long size);
}
