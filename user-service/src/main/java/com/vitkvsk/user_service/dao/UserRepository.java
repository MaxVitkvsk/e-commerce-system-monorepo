package com.vitkvsk.user_service.dao;

import com.vitkvsk.user_service.entities.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"cards"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET active = :active WHERE id = :id", nativeQuery = true)
    void updateActiveStatus(@Param("id") Long Id, @Param("active") boolean active);

}
