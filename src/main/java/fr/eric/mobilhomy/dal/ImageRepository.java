package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
}
