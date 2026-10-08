package com.wireblog.repository;

import com.wireblog.model.GroupPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, Long> {
    List<GroupPostComment> findByGroupPostIdOrderByCreatedAtAsc(Long groupPostId);
    Optional<GroupPostComment> findByIdAndGroupPostId(Long id, Long groupPostId);
    long countByGroupPostId(Long groupPostId);
    void deleteByGroupPostId(Long groupPostId);
    void deleteByParentId(Long parentId);
    void deleteByAuthorId(Long authorId);
}
