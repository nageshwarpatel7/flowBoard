package com.flowboard.card_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.service.CardService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CardController – MockMvc Tests")
class CardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean  CardService cardService;

    private static final String BASE    = "/api/v1/cards";
    private static final Long   USER_ID = 1L;

    private CardResponse sampleCard() {
        CardResponse r = new CardResponse();
        r.setId(1L); r.setListId(10L); r.setBoardId(100L);
        r.setTitle("Design login page"); r.setStatus(CardStatus.TO_DO);
        r.setPriority(Priority.MEDIUM); r.setPosition(0);
        r.setArchived(false); r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ── POST / ────────────────────────────────────────────────────────────────
    @Test @DisplayName("POST / → 201 created card")
    void create_201() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setListId(10L); req.setBoardId(100L); req.setTitle("Design login page");
        when(cardService.createCard(any(), eq(USER_ID))).thenReturn(sampleCard());

        mvc.perform(post(BASE).with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Design login page"))
                .andExpect(jsonPath("$.status").value("TO_DO"));
    }

    @Test @DisplayName("POST / → 400 missing X-User-Id header")
    void create_missingHeader_400() throws Exception {
        CreateCardRequest req = new CreateCardRequest();
        req.setListId(10L); req.setBoardId(100L); req.setTitle("X");

        mvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("GET /{id} → 200 existing card")
    void getById_200() throws Exception {
        when(cardService.getCardById(1L)).thenReturn(sampleCard());

        mvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Design login page"));
    }

    @Test @DisplayName("GET /{id} → 404 not found")
    void getById_404() throws Exception {
        when(cardService.getCardById(999L))
                .thenThrow(new CustomException("Card not found", HttpStatus.NOT_FOUND));

        mvc.perform(get(BASE + "/999")).andExpect(status().isNotFound());
    }

    // ── GET /list/{listId} ────────────────────────────────────────────────────
    @Test @DisplayName("GET /list/{listId} → 200 card list")
    void getByList_200() throws Exception {
        when(cardService.getCardByList(10L)).thenReturn(List.of(sampleCard()));

        mvc.perform(get(BASE + "/list/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listId").value(10));
    }

    @Test @DisplayName("GET /list/{listId} → 200 empty list")
    void getByList_empty() throws Exception {
        when(cardService.getCardByList(99L)).thenReturn(List.of());
        mvc.perform(get(BASE + "/list/99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /board/{boardId} ──────────────────────────────────────────────────
    @Test @DisplayName("GET /board/{boardId} → 200 board cards")
    void getByBoard_200() throws Exception {
        when(cardService.getCardByBoard(100L)).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/board/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].boardId").value(100));
    }

    // ── GET /assignee/{userId} ────────────────────────────────────────────────
    @Test @DisplayName("GET /assignee/{userId} → 200 assigned cards")
    void getByAssignee_200() throws Exception {
        CardResponse assigned = sampleCard(); assigned.setAssigneeId(2L);
        when(cardService.getCardByAssignee(2L)).thenReturn(List.of(assigned));
        mvc.perform(get(BASE + "/assignee/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assigneeId").value(2));
    }

    // ── PUT /{id} ─────────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id} → 200 updated card")
    void update_200() throws Exception {
        UpdateCardRequest req = new UpdateCardRequest();
        req.setTitle("Updated title"); req.setStatus(CardStatus.IN_PROGRESS);
        CardResponse updated = sampleCard();
        updated.setTitle("Updated title"); updated.setStatus(CardStatus.IN_PROGRESS);
        when(cardService.updateCard(eq(1L), any(), eq(USER_ID))).thenReturn(updated);

        mvc.perform(put(BASE + "/1").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @DisplayName("PUT /{id} → 400 update archived card")
    void update_archived_400() throws Exception {
        UpdateCardRequest req = new UpdateCardRequest(); req.setTitle("X");
        when(cardService.updateCard(eq(1L), any(), anyLong()))
                .thenThrow(new CustomException("Card is archived", HttpStatus.BAD_REQUEST));

        mvc.perform(put(BASE + "/1").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────
    @Test @DisplayName("DELETE /{id} → 200")
    void delete_200() throws Exception {
        doNothing().when(cardService).deleteCard(1L, USER_ID);
        mvc.perform(delete(BASE + "/1").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Card deleted successfully"));
    }

    @Test @DisplayName("DELETE /{id} → 404 not found")
    void delete_404() throws Exception {
        doThrow(new CustomException("Card not found", HttpStatus.NOT_FOUND))
                .when(cardService).deleteCard(eq(999L), anyLong());
        mvc.perform(delete(BASE + "/999").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── PUT /{id}/move ────────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/move → 200 card moved")
    void move_200() throws Exception {
        MoveCardRequest req = new MoveCardRequest();
        req.setTargetListId(20L); req.setTargetBoardId(100L);
        CardResponse moved = sampleCard(); moved.setListId(20L);
        when(cardService.moveCard(eq(1L), any(), eq(USER_ID))).thenReturn(moved);

        mvc.perform(put(BASE + "/1/move").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listId").value(20));
    }

    // ── PUT /{id}/archive ─────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/archive → 200 archived")
    void archive_200() throws Exception {
        CardResponse archived = sampleCard(); archived.setArchived(true);
        when(cardService.archiveCard(1L, USER_ID)).thenReturn(archived);
        mvc.perform(put(BASE + "/1/archive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test @DisplayName("PUT /{id}/archive → 400 already archived")
    void archive_alreadyArchived_400() throws Exception {
        when(cardService.archiveCard(eq(1L), anyLong()))
                .thenThrow(new CustomException("Already archived", HttpStatus.BAD_REQUEST));
        mvc.perform(put(BASE + "/1/archive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /{id}/unarchive ───────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/unarchive → 200 restored")
    void unarchive_200() throws Exception {
        CardResponse restored = sampleCard(); restored.setArchived(false);
        when(cardService.unarchiveCard(1L, USER_ID)).thenReturn(restored);
        mvc.perform(put(BASE + "/1/unarchive").with(csrf()).header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    // ── PUT /{id}/assignee ────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/assignee → 200 assigned")
    void setAssignee_200() throws Exception {
        AssignCardRequest req = new AssignCardRequest(); req.setAssigneeId(2L);
        CardResponse assigned = sampleCard(); assigned.setAssigneeId(2L);
        when(cardService.setAssignee(eq(1L), any(), eq(USER_ID))).thenReturn(assigned);

        mvc.perform(put(BASE + "/1/assignee").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(2));
    }

    @Test @DisplayName("PUT /{id}/assignee → 200 unassigned (null assigneeId)")
    void setAssignee_unassign_200() throws Exception {
        AssignCardRequest req = new AssignCardRequest(); req.setAssigneeId(null);
        CardResponse unassigned = sampleCard(); unassigned.setAssigneeId(null);
        when(cardService.setAssignee(eq(1L), any(), eq(USER_ID))).thenReturn(unassigned);

        mvc.perform(put(BASE + "/1/assignee").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── PUT /{id}/priority ────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/priority → 200 priority updated")
    void setPriority_200() throws Exception {
        SetPriorityRequest req = new SetPriorityRequest(); req.setPriority(Priority.HIGH);
        CardResponse high = sampleCard(); high.setPriority(Priority.HIGH);
        when(cardService.setPriority(eq(1L), any(), eq(USER_ID))).thenReturn(high);

        mvc.perform(put(BASE + "/1/priority").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    // ── PUT /{id}/status ──────────────────────────────────────────────────────
    @Test @DisplayName("PUT /{id}/status → 200 status updated")
    void setStatus_200() throws Exception {
        SetStatusRequest req = new SetStatusRequest(); req.setStatus(CardStatus.DONE);
        CardResponse done = sampleCard(); done.setStatus(CardStatus.DONE);
        when(cardService.setStatus(eq(1L), any(), eq(USER_ID))).thenReturn(done);

        mvc.perform(put(BASE + "/1/status").with(csrf())
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    // ── GET /board/{boardId}/status/{status} ──────────────────────────────────
    @Test @DisplayName("GET /board/{id}/status/{status} → 200 filtered by status")
    void getByStatus_200() throws Exception {
        when(cardService.getCardsByStatus(100L, CardStatus.TO_DO)).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/board/100/status/TO_DO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("TO_DO"));
    }

    // ── GET /board/{boardId}/priority/{priority} ──────────────────────────────
    @Test @DisplayName("GET /board/{id}/priority/{priority} → 200")
    void getByPriority_200() throws Exception {
        when(cardService.getCardsByPriority(100L, Priority.HIGH)).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/board/100/priority/HIGH"))
                .andExpect(status().isOk());
    }

    // ── GET /board/{boardId}/overdue ──────────────────────────────────────────
    @Test @DisplayName("GET /board/{id}/overdue → 200 overdue cards")
    void getOverdueByBoard_200() throws Exception {
        when(cardService.getOverdueCardsByBoard(100L)).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/board/100/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /overdue/all ──────────────────────────────────────────────────────
    @Test @DisplayName("GET /overdue/all → 200 all overdue cards")
    void getAllOverdue_200() throws Exception {
        when(cardService.getAllOverdueCards()).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/overdue/all"))
                .andExpect(status().isOk());
    }

    // ── GET /board/{boardId}/search ───────────────────────────────────────────
    @Test @DisplayName("GET /board/{id}/search → 200 search results")
    void searchByBoard_200() throws Exception {
        when(cardService.searchCards(100L, "login")).thenReturn(List.of(sampleCard()));
        mvc.perform(get(BASE + "/board/100/search").param("keyword", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Design login page"));
    }

    // ── GET /{id}/activity ────────────────────────────────────────────────────
    @Test @DisplayName("GET /{id}/activity → 200 activity log")
    void getActivity_200() throws Exception {
        when(cardService.getCardActivity(1L)).thenReturn(List.of());
        mvc.perform(get(BASE + "/1/activity")).andExpect(status().isOk());
    }

    // ── GET /{id}/activity/paged ──────────────────────────────────────────────
    @Test @DisplayName("GET /{id}/activity/paged → 200 paged activity")
    void getActivityPaged_200() throws Exception {
        PagedResponse<CardActivityResponse> paged = PagedResponse.<CardActivityResponse>builder()
                .content(List.of()).page(0).size(20).totalElements(0).totalPages(0).build();
        when(cardService.getCardActivityPaged(1L, 0, 20)).thenReturn(paged);

        mvc.perform(get(BASE + "/1/activity/paged").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── GET /board/{boardId}/stats ────────────────────────────────────────────
    @Test @DisplayName("GET /board/{id}/stats → 200 board statistics")
    void getBoardStats_200() throws Exception {
        BoardStatsResponse stats = BoardStatsResponse.builder()
                .boardId(100L).totalCards(10L).completedCards(4L)
                .completionRate(40.0).overdueCards(2L).build();
        when(cardService.getBoardStats(100L)).thenReturn(stats);

        mvc.perform(get(BASE + "/board/100/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(10))
                .andExpect(jsonPath("$.completionRate").value(40.0));
    }

    // ── POST /{id}/copy ───────────────────────────────────────────────────────
    @Test @DisplayName("POST /{id}/copy → 201 copied card")
    void copyCard_201() throws Exception {
        CardResponse copy = sampleCard(); copy.setId(99L); copy.setTitle("Copy of Design login page");
        when(cardService.getCardById(1L)).thenReturn(sampleCard());
        when(cardService.copyCard(eq(1L), anyLong(), eq(USER_ID))).thenReturn(copy);

        mvc.perform(post(BASE + "/1/copy").with(csrf())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Copy of Design login page"));
    }

    // ── GET /board/{boardId}/archived ─────────────────────────────────────────
    @Test @DisplayName("GET /board/{id}/archived → 200 archived cards")
    void getArchivedByBoard_200() throws Exception {
        CardResponse archived = sampleCard(); archived.setArchived(true);
        when(cardService.getArchivedCardsByBoard(100L)).thenReturn(List.of(archived));

        mvc.perform(get(BASE + "/board/100/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].archived").value(true));
    }

    // ── GET /list/{listId}/archived ───────────────────────────────────────────
    @Test @DisplayName("GET /list/{id}/archived → 200 archived cards in list")
    void getArchivedByList_200() throws Exception {
        when(cardService.getArchivedCardsByList(10L)).thenReturn(List.of());
        mvc.perform(get(BASE + "/list/10/archived")).andExpect(status().isOk());
    }
}