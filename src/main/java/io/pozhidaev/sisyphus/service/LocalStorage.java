package io.pozhidaev.sisyphus.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Slf4j
@Service
public class LocalStorage implements FileStorage {

    private Path fileDirectory;

    @Autowired
    public void setFileDirectory(Path fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    @Override
    public Mono<Integer> putObject(final Long id, final Flux<DataBuffer> parts) {
        return parts
            .map(v -> flushBufferToFile(v, id))
            .reduce(0, (a, v) -> a + v);
    }

    private int flushBufferToFile(final DataBuffer dataBuffer, final Long id){
        try {
            final InputStream inputStream = dataBuffer.asInputStream(true);
            int bytesLength = inputStream.available();
            final Path file = Paths.get(fileDirectory.toString(), id.toString());

            if (!Files.exists(file)) {
                Files.createFile(file);
            }

            Files.write(
                    file,
                    IOUtils.toByteArray(inputStream),
                    StandardOpenOption.APPEND
            );

            inputStream.close();
            return bytesLength;
        } catch (IOException e) {
            log.error("file write error");
            throw new RuntimeException("file write error", e);
        }
    }


}
