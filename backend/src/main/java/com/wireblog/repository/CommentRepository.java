package com.wireblog.repository;

import com.wireblog.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
    List<Comment> findByFlaggedTrueOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countGroupedByPostIds(@org.springframework.data.repository.query.Param("postIds") Collection<Long> postIds);
}
