package com.wireblog.repository;

import com.wireblog.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
    Page<Post> findByAuthorIdAndStatus(Long authorId, Post.PostStatus status, Pageable pageable);
    Page<Post> findByTagsContainingAndStatus(String tag, Post.PostStatus status, Pageable pageable);
    Page<Post> findByTitleContainingIgnoreCaseAndStatus(String title, Post.PostStatus status, Pageable pageable);
    boolean existsBySlug(String slug);
}
