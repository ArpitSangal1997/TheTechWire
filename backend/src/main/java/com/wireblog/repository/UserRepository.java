package com.wireblog.repository;

import com.wireblog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByHandle(String handle);
    Optional<User> findByVerificationToken(String verificationToken);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);

    @Query("select u from User u where u.enabled = true and u.id <> :excludeId and (lower(u.displayName) like lower(concat('%', :query, '%')) or lower(u.handle) like lower(concat('%', :query, '%')))")
    java.util.List<User> searchActiveUsers(@Param("query") String query, @Param("excludeId") Long excludeId, org.springframework.data.domain.Pageable pageable);

    @Query("select u from User u where lower(u.displayName) like lower(concat('%', :query, '%')) or lower(u.handle) like lower(concat('%', :query, '%')) or lower(u.email) like lower(concat('%', :query, '%')) order by u.createdAt desc")
    java.util.List<User> searchUsersForAdmin(@Param("query") String query);
}
