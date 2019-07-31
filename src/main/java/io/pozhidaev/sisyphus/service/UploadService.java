package io.pozhidaev.sisyphus.service;

import io.pozhidaev.sisyphus.domain.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.repository.PagingAndSortingRepository;
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
    private final PagingAndSortingRepository<File, Long> fileRepository;


    @Autowired
    public UploadService(
            final FileStorage fileStorage,
            final PagingAndSortingRepository<File, Long> fileRepository
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
            final long offset,
            final long length
    ) {
        //TODO Check content length
        return fileStorage
            .writeChunk(id, parts, offset)
            .map((e) -> {
                final File file = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File record not found."));
                log.info("[OLD OFFSET] {}", file.getContentOffset());
                log.info("[OFFSET] {}", file.getContentOffset() + length);
                file.setContentOffset(file.getContentOffset() + length);
                file.setLastUploadedChunkNumber(file.getLastUploadedChunkNumber() + 1);
                log.debug("File patching: {}", file);
                fileRepository.save(file);
                return file;
            });
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
        } catch (final IllegalArgumentException iae) {
            final RuntimeException exception = new RuntimeException(String.format("Invalid encoding :'%s'", str));
            log.warn("Invalid encoding :'{}'", str);
            throw exception;
        }
        return result;
    }

}
