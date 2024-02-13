package everyide.webide.container;


import everyide.webide.container.domain.Container;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContainerRepository extends JpaRepository<Container, Long> {
    Optional<Container> findByPath(String path);
}
