package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Auth;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Integer> {

    @EntityGraph(attributePaths = "utilisateur")
    Optional<Auth> findByLogin(String login);
}
