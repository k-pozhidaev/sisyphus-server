package io.pozhidaev.sisyphus.configurations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class SisyphusServerConfigurationTest {

    private SisyphusServerConfiguration sisyphusServerConfiguration;
    @Before
    public void setUp() {
        this.sisyphusServerConfiguration = new SisyphusServerConfiguration();
    }

    @Test
    public void authToken() {
        sisyphusServerConfiguration.setToken("test");
        assertEquals(sisyphusServerConfiguration.authToken().getLiteral(), "test");
    }

    @Test(expected = RuntimeException.class)
    public void authToken_exception() {
        sisyphusServerConfiguration.setToken(null);
        sisyphusServerConfiguration.authToken();
    }

    @Test
    public void fileDirectory() throws IOException {
        final Path fileDir = Files.createTempDirectory("fileDirectory").toAbsolutePath();
        sisyphusServerConfiguration.setFileDirectory(fileDir.toString() + "/test");
        sisyphusServerConfiguration.fileDirectory();
        assertTrue(Files.exists(Paths.get(fileDir.toString() , "test")));
        assertTrue(Files.isDirectory(Paths.get(fileDir.toString() + "/test")));
    }

    @Test(expected = AccessDeniedException.class)
    public void fileDirectory_exception() throws IOException {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        final Path fileDir = Files.createTempDirectory("fileDirectory", PosixFilePermissions.asFileAttribute(readOnly)).toAbsolutePath();
        sisyphusServerConfiguration.setFileDirectory(fileDir.toString());
        sisyphusServerConfiguration.fileDirectory();
    }

    @Test
    public void setSatToken() {
        sisyphusServerConfiguration.setToken("test");
        assertEquals(sisyphusServerConfiguration.getToken(), "test");
    }

    @Test
    public void getFileDirectory() {

        sisyphusServerConfiguration.setFileDirectory("test");
        assertEquals(sisyphusServerConfiguration.getFileDirectory(), "test");
    }

}