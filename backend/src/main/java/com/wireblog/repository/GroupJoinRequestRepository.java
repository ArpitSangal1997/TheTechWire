package com.wireblog.repository;

import com.wireblog.model.GroupJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    Optional<GroupJoinRequest> findByGroupIdAndUserId(Long groupId, Long userId);
    Optional<GroupJoinRequest> findByIdAndGroupId(Long id, Long groupId);
    List<GroupJoinRequest> findByGroupIdAndStatusOrderByCreatedAtAsc(Long groupId, GroupJoinRequest.Status status);
    long countByGroupIdAndStatus(Long groupId, GroupJoinRequest.Status status);
    void deleteByGroupId(Long groupId);
    void deleteByUserId(Long userId);
}
