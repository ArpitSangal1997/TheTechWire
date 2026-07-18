package com.wireblog.repository;

import com.wireblog.model.NewsHeadline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsHeadlineRepository extends JpaRepository<NewsHeadline, Long> {
    List<NewsHeadline> findTop50ByOrderByPublishedAtDesc();
    List<NewsHeadline> findTop50ByCategoryOrderByPublishedAtDesc(String category);
    void deleteByFetchedAtBefore(java.time.Instant cutoff);
}
