package com.flowBoard.list_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.service.ListService;
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

@WebMvcTest(ListController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ListController – MockMvc Tests")
class ListControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  ListService listService;

    private static final String BASE    = "/api/v1/lists";
    private static final Long   USER_ID = 1L;

    private ListResponse sample() {
        ListResponse r = new ListResponse();
        r.setId(1L); r.setBoardId(10L); r.setName("To Do");
        r.setPosition(0); r.setArchived(false);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ── POST / ────────────────────────────────────────────────────────────────
    @Test @DisplayName("POST / → 201 list created")
    void create_201() throws Exception {
        CreateListRequest req = new CreateListRequest();
        req.setBoardId(10L); req.setName("To Do");
        when(listService.createList(any(), eq(USER_ID))).thenReturn(sample());

        mvc.perform(post(BASE).with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("To Do"))
                .andExpect(jsonPath("$.boardId").value(10));
    }

    @Test @DisplayName("POST / → 400 missing X-User-Id")
    void create_missingHeader_400() throws Exception {
        CreateListRequest req = new CreateListRequest(); req.setBoardId(10L); req.setName("X");

        mvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("GET /{id} → 200 existing list")
    void getById_200() throws Exception {
        when(listService.getListById(1L)).thenReturn(sample());

        mvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("To Do"));
    }

    @Test @DisplayName("GET /{id} → 404 not found")
    void getById_404() throws Exception {
        when(listService.getListById(999L))
                .thenThrow(new CustomException("List not found", HttpStatus.NOT_FOUND));

        mvc.perform(get(BASE + "/999")).andExpect(status().isNotFound());
    }

    // ── GET /board/{boardId} ──────────────────────────────────────────────────
    @Test @DisplayName("GET /board/{boardId} → 200 all active lists")
    void getByBoard_200() throws Exception {
        when(listService.getListsByBoard(10L)).thenReturn(List.of(sample()));

        mvc.perform(get(BASE + "/board/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].boardId").value(10));
    }

    @Test @DisplayName("GET /board/{boardId} → 200 empty board")
    void getByBoard_empty() throws Exception {
        when(listService.getListsByBoard(99L)).thenReturn(List.of());
        mvc.perform(get(BASE + "/board/99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /board/{boardId}/archived ─────────────────────────────────────────
    @Test @DisplayName("GET /board/{boardId}/archived → 200 archived lists")
    void getArchived_200() throws Exception {
        ListResponse archived = sample(); archived.setArchived(true);
        when(listService.getArchivedLists(10L)).thenReturn(List.of(archived));

        mvc.perform(get(BASE + "/board/10/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].archived").value(true));
    }

    // ── PUT /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id} → 200 list updated")
    void update_200() throws Exception {
        UpdateListRequest req = new UpdateListRequest(); req.setName("Updated");
        ListResponse updated = sample(); updated.setName("Updated");
        when(listService.updateList(eq(1L), any(), eq(USER_ID))).thenReturn(updated);

        mvc.perform(put(BASE + "/1").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test @DisplayName("PUT /{id} → 404 list not found")
    void update_404() throws Exception {
        UpdateListRequest req = new UpdateListRequest(); req.setName("X");
        when(listService.updateList(eq(999L), any(), anyLong()))
                .thenThrow(new CustomException("List not found", HttpStatus.NOT_FOUND));

        mvc.perform(put(BASE + "/999").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────
    @Test @DisplayName("DELETE /{id} → 200 deleted")
    void delete_200() throws Exception {
        doNothing().when(listService).deleteList(1L, USER_ID);

        mvc.perform(delete(BASE + "/1").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("List deleted successfully"));
    }

    @Test @DisplayName("DELETE /{id} → 403 non-admin")
    void delete_403() throws Exception {
        doThrow(new CustomException("Forbidden", HttpStatus.FORBIDDEN))
                .when(listService).deleteList(eq(1L), eq(99L));

        mvc.perform(delete(BASE + "/1").with(csrf()).header("X-User-Id", 99L))
                .andExpect(status().isForbidden());
    }

    // ── PUT /{id}/archive ─────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/archive → 200 archived")
    void archive_200() throws Exception {
        ListResponse archived = sample(); archived.setArchived(true);
        when(listService.archiveList(1L, USER_ID)).thenReturn(archived);

        mvc.perform(put(BASE + "/1/archive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test @DisplayName("PUT /{id}/archive → 400 already archived")
    void archive_alreadyArchived_400() throws Exception {
        when(listService.archiveList(eq(1L), anyLong()))
                .thenThrow(new CustomException("Already archived", HttpStatus.BAD_REQUEST));

        mvc.perform(put(BASE + "/1/archive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /{id}/unarchive ───────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/unarchive → 200 restored")
    void unarchive_200() throws Exception {
        ListResponse restored = sample(); restored.setArchived(false);
        when(listService.unarchiveList(1L, USER_ID)).thenReturn(restored);

        mvc.perform(put(BASE + "/1/unarchive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    // ── PUT /reorder ──────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /reorder → 200 lists reordered")
    void reorder_200() throws Exception {
        ReorderListRequest req = new ReorderListRequest();
        req.setBoardId(10L); req.setOrderedListIds(List.of(2L, 1L));
        when(listService.reorderLists(any(), eq(USER_ID))).thenReturn(List.of(sample()));

        mvc.perform(put(BASE + "/reorder").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── PUT /{id}/move ────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/move → 200 list moved")
    void move_200() throws Exception {
        MoveListRequest req = new MoveListRequest(); 
        req.setTargetBoardId(10L);
        req.setTargetPosition(2);
        ListResponse moved = sample(); moved.setPosition(2);
        when(listService.moveList(eq(1L), any(), eq(USER_ID))).thenReturn(moved);

        mvc.perform(put(BASE + "/1/move").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));
    }
}