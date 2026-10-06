package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Link;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Integer> {
}
