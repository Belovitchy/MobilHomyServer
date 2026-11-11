package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Integer> {

    List<Link> findByOwnerIdOrderByNameAsc(Integer ownerId);

    /** Ne renvoie le lien que s'il appartient au propriétaire (contrôle de propriété). */
    Optional<Link> findByIdAndOwnerId(Integer id, Integer ownerId);
}
