package fr.eric.mobilhomy.bll;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Enregistre les photos de mobil-homes dans le dossier file.upload-dir (par défaut uploads/images/,
 * relatif au dossier de lancement du serveur), servi publiquement sur /images/** par WebMvcConfig.
 *
 * Sécurité :
 *  - le type est vérifié sur le CONTENU (signature binaire), pas sur l'extension ni le Content-Type
 *    envoyés par le client, qui sont falsifiables ;
 *  - le nom de fichier est généré (UUID) : aucun nom fourni par l'utilisateur n'est utilisé,
 *    donc pas de path traversal (../) ni d'écrasement d'un fichier existant ;
 *  - taille limitée à 5 Mo (ici + spring.servlet.multipart.max-file-size).
 */
@Service
public class ImageStorageService {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final String PUBLIC_PREFIX = "/images/";

    private final Path root;

    public ImageStorageService(@Value("${file.upload-dir:uploads/images/}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Enregistre l'image et renvoie son chemin public, ex : /images/3f2a…c9.webp */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image trop lourde (5 Mo maximum)");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier impossible", e);
        }

        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new IllegalArgumentException("Format non accepté : JPEG, PNG ou WebP uniquement");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) { // défense en profondeur, ne peut pas arriver avec un UUID
            throw new IllegalArgumentException("Chemin de fichier invalide");
        }

        try {
            Files.createDirectories(root);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Enregistrement de l'image impossible", e);
        }
        return PUBLIC_PREFIX + filename;
    }

    /** Détecte le format réel à partir des premiers octets (« magic numbers »). */
    static String detectExtension(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        // JPEG : FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG : 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        // WebP : "RIFF" .... "WEBP"
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
