package com.wireblog.repository;

import com.wireblog.model.NewsHeadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NewsHeadlineRepository extends JpaRepository<NewsHeadline, Long> {
    List<NewsHeadline> findTop50ByOrderByPublishedAtDesc();
    List<NewsHeadline> findTop50ByCategoryOrderByPublishedAtDesc(String category);
    void deleteByFetchedAtBefore(java.time.Instant cutoff);
    @Transactional
    void deleteByPublishedAtBefore(java.time.Instant cutoff);
}
