package com.wireblog.repository;

import com.wireblog.model.ReaderPulse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReaderPulseRepository extends JpaRepository<ReaderPulse, Long> {

    @Query("select rp.choice, count(rp) from ReaderPulse rp where rp.post.id = :postId group by rp.choice")
    List<Object[]> countByPostGroupedByChoice(@Param("postId") Long postId);

    void deleteByPostId(Long postId);
}
