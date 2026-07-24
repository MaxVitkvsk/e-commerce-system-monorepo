package com.vitkvsk.user_service.repository;

import com.vitkvsk.user_service.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"cards"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET active = :active, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

}
