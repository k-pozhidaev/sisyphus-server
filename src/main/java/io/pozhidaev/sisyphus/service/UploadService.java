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
        final Long fileSize,
        final String fileName,
        final String mimeType
    ){
        return Mono.fromSupplier(() -> File.builder()
                .mimeType(mimeType)
                .contentLength(fileSize)
                .originalName(fileName)
                .contentOffset(0L)
                .lastUploadedChunkNumber(0L)
                .build())
            .map(fileRepository::save)
            .flatMap(fileStorage::createFile);

    }


    public Mono<Long> uploadChunkAndGetUpdatedOffset(
            final Long id,
            final Flux<DataBuffer> parts
    ) {

        return fileStorage
            .putObject(id, parts)
            .map((e) -> {
                final File file = fileRepository.getOne(id);
                file.setContentOffset(file.getContentOffset() + e);
                file.setLastUploadedChunkNumber(file.getLastUploadedChunkNumber() + 1);
                fileRepository.save(file);
                fileRepository.flush();
                return file.getContentOffset();
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
        } catch (IllegalArgumentException iae) {
            log.warn("Invalid encoding :'{}'", str);
            throw new RuntimeException("Invalid encoding :'" + str + "'");
        }
        return result;
    }

}
