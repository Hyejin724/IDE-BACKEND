package everyide.webide.user;

import everyide.webide.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findAllByRefreshToken(String refreshToken);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE :roomId MEMBER OF u.roomsList")
    List<User> findUsersByRoomId(@Param("roomId") String roomId);

}

