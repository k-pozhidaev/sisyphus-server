package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.File;
import io.pozhidaev.sisyphus.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class UploadService {

    private final FileStorage fileStorage;
    private final FileRepository fileRepository;


    @Autowired
    public UploadService(
            final FileStorage fileStorage,
            final FileRepository fileRepository
            ) {
        this.fileStorage = fileStorage;
        this.fileRepository = fileRepository;
    }

    public Mono<File> createUpload(
        final File file
    ){
        return Mono.fromSupplier(() -> file)
            .map(fileRepository::save)
            .flatMap(fileStorage::createFile);

    }


    public Mono<File> uploadChunkAndGetUpdatedOffset(
            final Long id,
            final Flux<DataBuffer> parts,
            final long offset

    ) {
        //TODO Check content length
        return fileStorage
            .writeChunk(id, parts, offset)
            .map((e) -> {
                final File file = fileRepository.findById(id).orElseThrow(() -> new RuntimeException("File record not found."));
                file.setContentOffset(file.getContentOffset() + e);
                file.setLastUploadedChunkNumber(file.getLastUploadedChunkNumber() + 1);
                log.debug("File patching: {}", file);
                return file;
            })
            .map(fileRepository::save);
    }

    public Map<String, String> parseMetadata(final String metadata){
        return Arrays.stream(Objects.requireNonNull(metadata).split(","))
            .map(v -> v.split(" "))
            .collect(Collectors.toMap(e -> e[0], e -> this.b64DecodeUnicode(e[1])));
    }

    private String b64DecodeUnicode(final String str) {
        final byte[] value;
        final String result;
        try {
            value = Base64.getDecoder().decode(str);
            result = new String(value, UTF_8);
        } catch (IllegalArgumentException iae) {
            log.warn("Invalid encoding :'{}'", str);
            throw new RuntimeException("Invalid encoding :'" + str + "'");
        }
        return result;
    }

}
