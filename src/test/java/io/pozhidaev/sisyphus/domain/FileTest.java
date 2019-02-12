package io.pozhidaev.sisyphus.domain;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.Assert.*;

public class FileTest {

    @Test
    public void onPersist() {
        final File file = new File();
        file.onPersist();
        assertEquals(file.getCreatedAt().getHour(), LocalDateTime.now(ZoneId.systemDefault()).getHour());
        assertEquals(file.getUpdatedAt().getHour(), LocalDateTime.now(ZoneId.systemDefault()).getHour());
    }

    @Test
    public void onUpdate() {
        final File file = new File();
        file.onUpdate();
        assertEquals(file.getUpdatedAt().getHour(), LocalDateTime.now(ZoneId.systemDefault()).getHour());

    }
}