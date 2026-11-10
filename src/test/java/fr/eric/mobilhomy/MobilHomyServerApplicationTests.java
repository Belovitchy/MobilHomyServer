package fr.eric.mobilhomy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Vérifie que le contexte Spring démarre — sur H2 (profil "test"), donc sans SQL Server, y compris en CI. */
@SpringBootTest
@ActiveProfiles("test")
class MobilHomyServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
