package io.pozhidaev.sisyphus.service;


import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface FileStorage {
    Mono<Integer> putObject(final Long id, final Flux<DataBuffer> parts);
}
