package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entities.EntryApproval;
import com.marine.management.modules.finance.domain.enums.ApprovalLevel;
import com.marine.management.modules.finance.domain.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntryApprovalRepository extends JpaRepository<EntryApproval, UUID> {

    /**
     * Find all approvals for an entry (ordered by level)
     */
    List<EntryApproval> findByEntry_IdOrderByApprovalLevelAsc(UUID entryId);

    /**
     * Find specific approval level for an entry
     */
    Optional<EntryApproval> findByEntry_IdAndApprovalLevel(UUID entryId, ApprovalLevel level);

    /**
     * Find pending approvals for specific level
     */
    @Query("""
        SELECT a FROM EntryApproval a
        WHERE a.approvalLevel = :level
        AND a.approvalStatus = 'PENDING'
        ORDER BY a.createdAt DESC
    """)
    List<EntryApproval> findPendingByLevel(@Param("level") ApprovalLevel level);

    /**
     * Count pending approvals for level
     */
    long countByApprovalLevelAndApprovalStatus(ApprovalLevel level, ApprovalStatus status);

    /**
     * Find approvals by approver
     */
    List<EntryApproval> findByApprover_IdOrderByApprovalDateDesc(UUID approverId);

    /**
     * Find rejected approvals
     */
    @Query("""
        SELECT a FROM EntryApproval a
        WHERE a.approvalStatus = 'REJECTED'
        ORDER BY a.approvalDate DESC
    """)
    List<EntryApproval> findRejected();
}