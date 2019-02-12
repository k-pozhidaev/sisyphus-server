package io.pozhidaev.sisyphus.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class LocalStorage implements FileStorage {

    private Path fileDirectory;
    private Function<Path, Mono<AsynchronousFileChannel>> channelFunction;

    @Autowired
    public void setFileDirectory(Path fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    @Autowired
    public void setChannelFunction(Function<Path, Mono<AsynchronousFileChannel>> channelFunction) {
        this.channelFunction = channelFunction;
    }

    @Override
    public Mono<Integer> putObject(final Long id, final Flux<DataBuffer> parts) {
        return parts
            .map(v -> flushBufferToFile(v, id))
            .reduce(0, Integer::sum);
    }

    public Mono<Path> createFile(final Long id) {
        return Mono.fromSupplier(() -> {
            try {
                return Files.createFile(Paths.get(fileDirectory.toString(), id.toString()));
            } catch (IOException e) {
                throw new RuntimeException("File creation failed: " + id);
            }
        });
    }

    public Mono<Integer> writeChunk(final Long id, final Flux<DataBuffer> parts, final long offset, final long size) {

        final Path file = Paths.get(fileDirectory.toString(), requireNonNull(id).toString());
        final Mono<AsynchronousFileChannel> channel = channelFunction.apply(file);

        final Mono<Integer> writeAndGetWritten = channel
            .flatMapMany(asynchronousFileChannel -> DataBufferUtils.write(requireNonNull(parts), asynchronousFileChannel, offset))
            .map(dataBuffer -> {
                final int capacity = dataBuffer.capacity();
                DataBufferUtils.release(dataBuffer);
                return capacity;
            })
            .reduce(Integer::sum)
            .doOnSuccessOrError((integer, throwable) -> channel.subscribe(this::closeChannel));


        return requireNonNull(parts)
            .map(DataBuffer::capacity)
            .reduce(Integer::sum)
            .map(integer -> {
                if (!Long.valueOf(integer.longValue()).equals(size)) {
                    throw new RuntimeException("Buffer not equals size");
                }
                return integer;
            })
            .then(writeAndGetWritten);

    }

    void closeChannel(final AsynchronousFileChannel asynchronousFileChannel){
        try {
            asynchronousFileChannel.close();
        } catch (IOException e) {
            throw new RuntimeException("Channel close error");
        }
    }

    private int flushBufferToFile(final DataBuffer dataBuffer, final Long id) {
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
