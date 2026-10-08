package com.wireblog.repository;

import com.wireblog.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
    List<Comment> findByFlaggedTrueOrderByCreatedAtDesc();
    void deleteByPostId(Long postId);
    void deleteByAuthorId(Long authorId);
    void deleteByParentId(Long parentId);

    @Modifying
    @Query("delete from Comment c where c.parent.author.id = :authorId")
    void deleteRepliesToAuthor(@Param("authorId") Long authorId);

    @Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countGroupedByPostIds(@Param("postIds") Collection<Long> postIds);
}
