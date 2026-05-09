package com.flowboard.workspace_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.*;
import com.flowboard.workspace_service.enums.*;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.service.WorkspaceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WorkspaceController – MockMvc Tests")
class WorkspaceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  WorkspaceService workspaceService;

    private static final String BASE    = "/api/v1/workspaces";
    private static final Long   USER_ID = 1L;
    private static final String ROLE    = "MEMBER";
    private static final String ADMIN   = "PLATFORM_ADMIN";

    private WorkspaceResponse sampleWs() {
        WorkspaceResponse r = new WorkspaceResponse();
        r.setId(1L); r.setName("Dev Team"); r.setOwnerId(USER_ID);
        r.setVisibility(Visibility.PRIVATE);
        r.setMembers(List.of()); r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private WorkspaceMember sampleMember() {
        return WorkspaceMember.builder()
                .id(1L).userId(USER_ID).role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now()).build();
    }

    // ── POST / ────────────────────────────────────────────────────────────────
    @Test @DisplayName("POST / → 201 workspace created")
    void create_201() throws Exception {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team"); req.setVisibility(Visibility.PRIVATE);
        when(workspaceService.createWorkspace(any(), eq(USER_ID))).thenReturn(sampleWs());

        mvc.perform(post(BASE).with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dev Team"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test @DisplayName("POST / → 400 duplicate workspace name")
    void create_duplicate_400() throws Exception {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Dev Team"); req.setVisibility(Visibility.PRIVATE);
        when(workspaceService.createWorkspace(any(), anyLong()))
                .thenThrow(new CustomException("Workspace name already exists", HttpStatus.BAD_REQUEST));

        mvc.perform(post(BASE).with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST / → 400 missing X-User-Id")
    void create_missingHeader_400() throws Exception {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest(); req.setName("X");

        mvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("GET /{id} → 200 existing workspace")
    void getById_200() throws Exception {
        when(workspaceService.getById(1L, USER_ID, ROLE)).thenReturn(sampleWs());

        mvc.perform(get(BASE + "/1")
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dev Team"));
    }

    @Test @DisplayName("GET /{id} → 404 not found")
    void getById_404() throws Exception {
        when(workspaceService.getById(eq(999L), anyLong(), any()))
                .thenThrow(new CustomException("Workspace not found", HttpStatus.NOT_FOUND));

        mvc.perform(get(BASE + "/999")
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("GET /{id} → 403 private workspace non-member")
    void getById_private_403() throws Exception {
        when(workspaceService.getById(eq(1L), eq(99L), any()))
                .thenThrow(new CustomException("Access denied", HttpStatus.FORBIDDEN));

        mvc.perform(get(BASE + "/1")
                        .header("X-User-Id", 99L).header("X-User-Role", ROLE))
                .andExpect(status().isForbidden());
    }

    // ── GET /owner/{ownerId} ──────────────────────────────────────────────────
    @Test @DisplayName("GET /owner/{ownerId} → 200 owned workspaces")
    void getByOwner_200() throws Exception {
        when(workspaceService.getByOwner(USER_ID)).thenReturn(List.of(sampleWs()));

        mvc.perform(get(BASE + "/owner/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value(1));
    }

    @Test @DisplayName("GET /owner/{ownerId} → 200 empty list")
    void getByOwner_empty() throws Exception {
        when(workspaceService.getByOwner(99L)).thenReturn(List.of());
        mvc.perform(get(BASE + "/owner/99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /member/{userId} ──────────────────────────────────────────────────
    @Test @DisplayName("GET /member/{userId} → 200 member workspaces")
    void getByMember_200() throws Exception {
        when(workspaceService.getByMember(USER_ID)).thenReturn(List.of(sampleWs()));

        mvc.perform(get(BASE + "/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ── GET /public ───────────────────────────────────────────────────────────
    @Test @DisplayName("GET /public → 200 public workspaces")
    void getPublic_200() throws Exception {
        WorkspaceResponse pub = sampleWs(); pub.setVisibility(Visibility.PUBLIC);
        when(workspaceService.getPublicWorkspaces()).thenReturn(List.of(pub));

        mvc.perform(get(BASE + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visibility").value("PUBLIC"));
    }

    // ── PUT /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id} → 200 updated")
    void update_200() throws Exception {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest(); req.setName("Updated Team");
        WorkspaceResponse updated = sampleWs(); updated.setName("Updated Team");
        when(workspaceService.updateWorkspace(eq(1L), any(), eq(USER_ID), eq(ROLE))).thenReturn(updated);

        mvc.perform(put(BASE + "/1").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Team"));
    }

    @Test @DisplayName("PUT /{id} → 403 non-admin update")
    void update_403() throws Exception {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest(); req.setName("Hack");
        when(workspaceService.updateWorkspace(anyLong(), any(), anyLong(), any()))
                .thenThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN));

        mvc.perform(put(BASE + "/1").with(csrf())
                        .header("X-User-Id", 99L).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────
    @Test @DisplayName("DELETE /{id} → 200 deleted")
    void delete_200() throws Exception {
        doNothing().when(workspaceService).deleteWorkspace(1L, USER_ID, ROLE);

        mvc.perform(delete(BASE + "/1").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("Workspace deleted successfully"));
    }

    @Test @DisplayName("DELETE /{id} → 403 non-owner delete")
    void delete_403() throws Exception {
        doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                .when(workspaceService).deleteWorkspace(anyLong(), eq(99L), any());

        mvc.perform(delete(BASE + "/1").with(csrf())
                        .header("X-User-Id", 99L).header("X-User-Role", ROLE))
                .andExpect(status().isForbidden());
    }

    // ── POST /{id}/members ────────────────────────────────────────────────────
    @Test @DisplayName("POST /{id}/members → 201 member added")
    void addMember_201() throws Exception {
        AddMemberRequest req = new AddMemberRequest();
        req.setUserId(3L); req.setRole(MemberRole.MEMBER);
        when(workspaceService.addMember(eq(1L), any(), eq(USER_ID), eq(ROLE))).thenReturn(sampleMember());

        mvc.perform(post(BASE + "/1/members").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test @DisplayName("POST /{id}/members → 400 already member")
    void addMember_alreadyMember_400() throws Exception {
        AddMemberRequest req = new AddMemberRequest(); req.setUserId(1L);
        when(workspaceService.addMember(anyLong(), any(), anyLong(), any()))
                .thenThrow(new CustomException("Already a member", HttpStatus.BAD_REQUEST));

        mvc.perform(post(BASE + "/1/members").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /{id}/member/{memberId} ────────────────────────────────────────
    @Test @DisplayName("DELETE /{id}/member/{memberId} → 200")
    void removeMember_200() throws Exception {
        doNothing().when(workspaceService).removeMember(1L, 3L, USER_ID, ROLE);

        mvc.perform(delete(BASE + "/1/member/3").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("Member removed successfully"));
    }

    // ── GET /{id}/members ─────────────────────────────────────────────────────
    @Test @DisplayName("GET /{id}/members → 200 member list")
    void getMembers_200() throws Exception {
        when(workspaceService.getMembers(1L)).thenReturn(List.of(sampleMember()));

        mvc.perform(get(BASE + "/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    // ── POST /{id}/invite ─────────────────────────────────────────────────────
    @Test @DisplayName("POST /{id}/invite → 200 invitation sent")
    void invite_200() throws Exception {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("new@ex.com"); req.setRole(MemberRole.MEMBER);
        doNothing().when(workspaceService).inviteMember(eq(1L), any(), eq(USER_ID), eq(ROLE));

        mvc.perform(post(BASE + "/1/invite").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation sent to new@ex.com"));
    }

    @Test @DisplayName("POST /{id}/invite → 400 duplicate pending invite")
    void invite_duplicate_400() throws Exception {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("dup@ex.com"); req.setRole(MemberRole.MEMBER);
        doThrow(new CustomException("Pending invite exists", HttpStatus.BAD_REQUEST))
                .when(workspaceService).inviteMember(anyLong(), any(), anyLong(), any());

        mvc.perform(post(BASE + "/1/invite").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /invite/accept ────────────────────────────────────────────────────
    @Test @DisplayName("GET /invite/accept → 200 accepted")
    void acceptInvite_200() throws Exception {
        doNothing().when(workspaceService).acceptInvitation("valid-token", USER_ID);

        mvc.perform(get(BASE + "/invite/accept")
                        .param("token", "valid-token").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation accepted! You have joined the workspace."));
    }

    @Test @DisplayName("GET /invite/accept → 404 invalid token")
    void acceptInvite_invalidToken_404() throws Exception {
        doThrow(new CustomException("Token not found", HttpStatus.NOT_FOUND))
                .when(workspaceService).acceptInvitation(eq("bad"), anyLong());

        mvc.perform(get(BASE + "/invite/accept")
                        .param("token", "bad").header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("GET /invite/accept → 400 expired invite")
    void acceptInvite_expired_400() throws Exception {
        doThrow(new CustomException("Invite expired", HttpStatus.BAD_REQUEST))
                .when(workspaceService).acceptInvitation(eq("expired-token"), anyLong());

        mvc.perform(get(BASE + "/invite/accept")
                        .param("token", "expired-token").header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /{id}/invitations/{invId} ──────────────────────────────────────
    @Test @DisplayName("DELETE /{id}/invitations/{invId} → 200 revoked")
    void revokeInvite_200() throws Exception {
        doNothing().when(workspaceService).revokeInvitation(1L, 5L, USER_ID, ROLE);

        mvc.perform(delete(BASE + "/1/invitations/5").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation revoked"));
    }

    // ── GET /{id}/invitations ─────────────────────────────────────────────────
    @Test @DisplayName("GET /{id}/invitations → 200 pending list")
    void getPendingInvitations_200() throws Exception {
        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id(1L).workspaceId(1L).inviteeEmail("new@ex.com")
                .status("PENDING").role(MemberRole.MEMBER).token("tok")
                .createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7)).build();
        when(workspaceService.getPendingInvitations(1L, USER_ID, ROLE)).thenReturn(List.of(inv));

        mvc.perform(get(BASE + "/1/invitations")
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inviteeEmail").value("new@ex.com"));
    }

    // ── PUT /{id}/members/{memberId}/role ─────────────────────────────────────
    @Test @DisplayName("PUT /{id}/members/{memberId}/role → 200")
    void updateMemberRole_200() throws Exception {
        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest(); req.setRole(MemberRole.ADMIN);
        doNothing().when(workspaceService).updateMemberRole(eq(1L), eq(3L), any(), eq(USER_ID), eq(ROLE));

        mvc.perform(put(BASE + "/1/members/3/role").with(csrf())
                        .header("X-User-Id", USER_ID).header("X-User-Role", ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Member role updated successfully"));
    }
}