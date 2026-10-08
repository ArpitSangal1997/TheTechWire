package com.wireblog.repository;

import com.wireblog.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    @Query("select f from Friendship f where (f.requester.id = :firstId and f.recipient.id = :secondId) or (f.requester.id = :secondId and f.recipient.id = :firstId)")
    Optional<Friendship> findBetween(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    List<Friendship> findByRequesterIdOrRecipientId(Long requesterId, Long recipientId);
    void deleteByRequesterIdOrRecipientId(Long requesterId, Long recipientId);
}
