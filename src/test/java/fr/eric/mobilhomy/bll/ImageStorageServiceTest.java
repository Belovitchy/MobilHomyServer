package fr.eric.mobilhomy.bll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Upload des photos : écrit dans un dossier temporaire, jamais dans le vrai uploads/images. */
class ImageStorageServiceTest {

    /** En-tête PNG minimal (signature + début du chunk IHDR) : suffisant pour la détection. */
    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };
    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1
    };
    private static final byte[] WEBP = {
            'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    @TempDir
    Path tempDir;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(tempDir.toString());
    }

    @Test
    void store_png_enregistreLeFichierEtRenvoieLeCheminPublic() throws Exception {
        String path = service.store(new MockMultipartFile("file", "photo.png", "image/png", PNG));

        assertTrue(path.matches("^/images/[0-9a-f-]{36}\\.png$"), path);
        Path saved = tempDir.resolve(path.substring("/images/".length()));
        assertArrayEquals(PNG, Files.readAllBytes(saved));
    }

    @Test
    void store_detecteLeFormatSurLeContenuEtPasSurLeNom() {
        // Un vrai JPEG nommé .png est enregistré en .jpg
        String path = service.store(new MockMultipartFile("file", "trompeur.png", "image/png", JPEG));
        assertTrue(path.endsWith(".jpg"), path);

        assertTrue(service.store(new MockMultipartFile("file", "x", null, WEBP)).endsWith(".webp"));
    }

    @Test
    void store_fichierTexteDeguiseEnImage_refuse() {
        byte[] script = "<script>alert('xss')</script>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fake = new MockMultipartFile("file", "photo.png", "image/png", script);

        assertThrows(IllegalArgumentException.class, () -> service.store(fake));
        assertEquals(0, tempDir.toFile().list().length, "Aucun fichier ne doit être écrit");
    }

    @Test
    void store_nomMalveillant_ignoreLeNomFourni() {
        String path = service.store(new MockMultipartFile("file", "../../application.properties", "image/png", PNG));

        assertFalse(path.contains(".."), path);
        assertEquals(1, tempDir.toFile().list().length);
    }

    @Test
    void store_fichierVide_refuse() {
        MockMultipartFile empty = new MockMultipartFile("file", "vide.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.store(empty));
    }

    @Test
    void store_fichierTropLourd_refuse() {
        byte[] big = new byte[(int) ImageStorageService.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        assertThrows(IllegalArgumentException.class,
                () -> service.store(new MockMultipartFile("file", "big.png", "image/png", big)));
    }
}
