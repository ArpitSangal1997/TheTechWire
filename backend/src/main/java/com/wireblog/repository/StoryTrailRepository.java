package com.wireblog.repository;

import com.wireblog.model.StoryTrail;
import com.wireblog.model.Post;
import com.wireblog.model.CommunityGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoryTrailRepository extends JpaRepository<StoryTrail, Long> {
    Optional<StoryTrail> findBySlug(String slug);
    Optional<StoryTrail> findByTitleIgnoreCase(String title);
    boolean existsBySlug(String slug);

        @Query(value = "select t from StoryTrail t where exists (select p.id from Post p where p.trail = t and p.status = :status) or exists (select gp.id from GroupPost gp where gp.trail = t and gp.group.visibility = :visibility)",
            countQuery = "select count(t) from StoryTrail t where exists (select p.id from Post p where p.trail = t and p.status = :status) or exists (select gp.id from GroupPost gp where gp.trail = t and gp.group.visibility = :visibility)")
        Page<StoryTrail> findTrailsWithPublicContent(@Param("status") Post.PostStatus status,
                                                     @Param("visibility") CommunityGroup.Visibility visibility,
                                                     Pageable pageable);
}
