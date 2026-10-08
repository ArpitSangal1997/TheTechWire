package com.wireblog.repository;
import com.wireblog.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
	List<GroupMember> findByGroupId(Long groupId);
	List<GroupMember> findByUserId(Long userId);
	long countByGroupId(Long groupId);
	void deleteByGroupId(Long groupId);
	void deleteByUserId(Long userId);
}
