package com.flowboard.board_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("BoardController – MockMvc Tests")
class BoardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean BoardService boardService;

    private static final String BASE = "/api/v1/boards";
    private static final Long USER_ID = 1L;

    private BoardResponse sampleResponse() {
        return BoardResponse.builder()
                .id(10L)
                .name("Test Board")
                .workspaceId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST / → 201 board created")
    void create_201() throws Exception {
        CreateBoardRequest req = new CreateBoardRequest();
        req.setName("Test Board");
        req.setWorkspaceId(1L);

        when(boardService.createBoard(any(), eq(USER_ID))).thenReturn(sampleResponse());

        mvc.perform(post(BASE).with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Test Board"));
    }

    @Test
    @DisplayName("POST / → 400 missing header")
    void create_missingHeader_400() throws Exception {
        CreateBoardRequest req = new CreateBoardRequest();
        req.setName("Test Board");
        req.setWorkspaceId(1L);

        mvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /{id} → 200 board found")
    void getById_200() throws Exception {
        when(boardService.getBoardById(10L, USER_ID)).thenReturn(sampleResponse());

        mvc.perform(get(BASE + "/10")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /workspace/{workspaceId} → 200")
    void getByWorkspace_200() throws Exception {
        when(boardService.getBoardsByWorkspace(1L, USER_ID)).thenReturn(List.of(sampleResponse()));

        mvc.perform(get(BASE + "/workspace/1")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /member/{userId} → 200")
    void getByMember_200() throws Exception {
        when(boardService.getBoardsByMember(USER_ID)).thenReturn(List.of(sampleResponse()));

        mvc.perform(get(BASE + "/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /creator/{createdById} → 200")
    void getByCreator_200() throws Exception {
        when(boardService.getBoardsByCreator(USER_ID)).thenReturn(List.of(sampleResponse()));

        mvc.perform(get(BASE + "/creator/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /public → 200")
    void getPublic_200() throws Exception {
        when(boardService.getPublicBoards()).thenReturn(List.of(sampleResponse()));

        mvc.perform(get(BASE + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /workspace/{workspaceId}/closed → 200")
    void getClosedBoards_200() throws Exception {
        when(boardService.getClosedBoards(1L, USER_ID)).thenReturn(List.of(sampleResponse()));

        mvc.perform(get(BASE + "/workspace/1/closed")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("PUT /{id} → 200 updated")
    void update_200() throws Exception {
        UpdateBoardRequest req = new UpdateBoardRequest();
        req.setName("Updated");

        BoardResponse res = BoardResponse.builder()
                .id(10L)
                .name("Updated")
                .workspaceId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(boardService.updateBoard(eq(10L), any(), eq(USER_ID))).thenReturn(res);

        mvc.perform(put(BASE + "/10").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PUT /{id}/close → 200")
    void close_200() throws Exception {
        when(boardService.closeBoard(10L, USER_ID)).thenReturn(sampleResponse());

        mvc.perform(put(BASE + "/10/close").with(csrf())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id}/reopen → 200")
    void reopen_200() throws Exception {
        when(boardService.reopenBoard(10L, USER_ID)).thenReturn(sampleResponse());

        mvc.perform(put(BASE + "/10/reopen").with(csrf())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /{id} → 200 deleted")
    void delete_200() throws Exception {
        doNothing().when(boardService).deleteBoard(10L, USER_ID);

        mvc.perform(delete(BASE + "/10").with(csrf())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Board deleted successfully"));
    }

    @Test
    @DisplayName("POST /{id}/members → 200")
    void addMember_200() throws Exception {
        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId(2L);

        BoardMember member = new BoardMember();
        member.setId(1L);

        when(boardService.addMember(eq(10L), any(), eq(USER_ID))).thenReturn(member);

        mvc.perform(post(BASE + "/10/members").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /{id}/members/{memberId} → 200")
    void deleteMember_200() throws Exception {
        doNothing().when(boardService).removeMember(10L, 2L, USER_ID);

        mvc.perform(delete(BASE + "/10/members/2").with(csrf())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id}/members/{memberId}/role → 200")
    void updateMemberRole_200() throws Exception {
        UpdateBoardMemberRoleRequest req = new UpdateBoardMemberRoleRequest();
        req.setRole(com.flowboard.board_service.enums.BoardMemberRole.MEMBER);

        doNothing().when(boardService).updateMemberRole(eq(10L), eq(2L), any(), eq(USER_ID));

        mvc.perform(put(BASE + "/10/members/2/role").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id}/members → 200")
    void getMembers_200() throws Exception {
        BoardMember member = new BoardMember();
        member.setId(1L);

        when(boardService.getMembers(10L)).thenReturn(List.of(member));

        mvc.perform(get(BASE + "/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /{id}/analytics → 200")
    void getAnalytics_200() throws Exception {
        BoardResponse.BoardAnalytics analytics = BoardResponse.BoardAnalytics.builder()
                .totalMembers(0)
                .adminCount(0)
                .memberCount(0)
                .observerCount(0)
                .build();
        when(boardService.getBoardAnalytics(10L, USER_ID)).thenReturn(analytics);

        mvc.perform(get(BASE + "/10/analytics")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk());
    }
}
