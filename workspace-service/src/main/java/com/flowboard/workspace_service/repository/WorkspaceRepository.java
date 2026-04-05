package com.flowboard.workspace_service.repository;

import com.flowboard.workspace_service.entity.Workspace;
import com.flowboard.workspace_service.enums.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    // All workspace owned by a user
    List<Workspace> findByOwnerId(Long ownerId);

    // All workspace where user is a member (via WorkspaceMember)
    @Query("Select w from Workspace w JOIN w.members m Where m.userId= :userId")
    List<Workspace> findByMemberUserId(@Param("userId") Long userId);

    // All public workspaces
    List<Workspace> findByVisibility(Visibility visibility);

    //Check duplicate name per owner
    boolean existsByNameAndOwnerId(String name, Long ownerId);

    // Count workspace owned by a user
    long countByOwnerId(Long ownerId);
}
