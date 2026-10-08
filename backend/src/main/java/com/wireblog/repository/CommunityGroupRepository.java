package com.wireblog.repository;
import com.wireblog.model.CommunityGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommunityGroupRepository extends JpaRepository<CommunityGroup, Long> {
	boolean existsBySlug(String slug);
	List<CommunityGroup> findAllByOrderByCreatedAtDesc();
	List<CommunityGroup> findByCreatorId(Long creatorId);
}
