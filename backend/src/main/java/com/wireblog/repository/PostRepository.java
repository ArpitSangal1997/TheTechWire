package com.wireblog.repository;

import com.wireblog.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
    List<Post> findAllByAuthorId(Long authorId);
    Page<Post> findByAuthorIdAndStatus(Long authorId, Post.PostStatus status, Pageable pageable);
    Page<Post> findByTrailIdAndStatus(Long trailId, Post.PostStatus status, Pageable pageable);
    Page<Post> findByTagsContainingAndStatus(String tag, Post.PostStatus status, Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCaseAndStatus(String title, Post.PostStatus status, Pageable pageable);
    @Query("select p from Post p join p.author a where lower(p.title) like lower(concat('%', :query, '%')) or lower(a.displayName) like lower(concat('%', :query, '%')) or lower(a.handle) like lower(concat('%', :query, '%'))")
    Page<Post> searchForAdmin(@Param("query") String query, Pageable pageable);
    long countByTrailIdAndStatus(Long trailId, Post.PostStatus status);
    boolean existsBySlug(String slug);
}
