package com.wireblog.repository;

import com.wireblog.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareRepository extends JpaRepository<Share, Long> {
    List<Share> findBySharedToIdOrderByCreatedAtDesc(Long userId);
    List<Share> findByPostId(Long postId);
    void deleteByPostId(Long postId);
    void deleteBySharedById(Long userId);
    void deleteBySharedToId(Long userId);
}
