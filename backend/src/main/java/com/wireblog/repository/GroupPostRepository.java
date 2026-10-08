package com.wireblog.repository;

import com.wireblog.model.GroupPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {
    List<GroupPost> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    Optional<GroupPost> findByIdAndGroupId(Long id, Long groupId);
    List<GroupPost> findByTrailIdOrderByCreatedAtDesc(Long trailId);
    long countByTrailId(Long trailId);
    void deleteByGroupId(Long groupId);
    void deleteByAuthorId(Long authorId);
}
