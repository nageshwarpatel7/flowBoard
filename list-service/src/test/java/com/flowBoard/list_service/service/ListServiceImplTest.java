package com.flowBoard.list_service.service;

import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.entity.TaskList;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.repository.ListRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListServiceImpl Unit Tests")
class ListServiceImplTest {

    @Mock ListRepository listRepository;
    @InjectMocks ListServiceImpl listService;

    private TaskList sampleList;

    @BeforeEach
    void setUp() {
        sampleList = TaskList.builder()
                .id(1L).boardId(10L).name("To Do")
                .position(0).isArchived(false)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("createList should append at end when position is null")
    void createList_appendsAtEnd() {
        CreateListRequest req = new CreateListRequest();
        req.setBoardId(10L);
        req.setName("To Do");
        req.setPosition(null);

        // FIX: anyLong() instead of any() for Long parameter — avoids NPE on unboxing
        when(listRepository.findMaxPositionByBoardId(anyLong()))
                .thenReturn(Optional.of(2));
        when(listRepository.save(any())).thenReturn(sampleList);

        ListResponse response = listService.createList(req, 1L);

        assertThat(response).isNotNull();
        // FIX: anyLong(), anyInt() instead of any(), any()
        verify(listRepository, never()).shiftPositionsRight(anyLong(), anyInt());
    }

    @Test
    @DisplayName("createList should shift right and insert at given position")
    void createList_insertsAtPosition() {
        CreateListRequest req = new CreateListRequest();
        req.setBoardId(10L);
        req.setName("Review");
        req.setPosition(1);

        when(listRepository.save(any())).thenReturn(sampleList);

        listService.createList(req, 1L);

        verify(listRepository).shiftPositionsRight(10L, 1);
    }

    @Test
    @DisplayName("archiveList should shift siblings left and set isArchived=true")
    void archiveList_success() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));
        when(listRepository.save(any())).thenReturn(sampleList);

        listService.archiveList(1L, 1L);

        assertThat(sampleList.isArchived()).isTrue();
        verify(listRepository).shiftPositionsLeft(sampleList.getBoardId(),
                sampleList.getPosition());
    }

    @Test
    @DisplayName("archiveList should throw 400 when already archived")
    void archiveList_alreadyArchived_throws() {
        sampleList.setArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));

        assertThatThrownBy(() -> listService.archiveList(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("deleteList should shift siblings left and delete")
    void deleteList_success() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));

        listService.deleteList(1L, 1L);

        verify(listRepository).shiftPositionsLeft(
                sampleList.getBoardId(), sampleList.getPosition());
        verify(listRepository).delete(sampleList);
    }

    @Test
    @DisplayName("getListById should throw 404 when not found")
    void getListById_notFound_throws() {
        when(listRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.getListById(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("unarchiveList should restore and append at end of active lists")
    void unarchiveList_success() {
        sampleList.setArchived(true);
        sampleList.setPosition(0);

        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));
        when(listRepository.findMaxPositionByBoardId(anyLong()))
                .thenReturn(Optional.of(3));
        when(listRepository.save(any())).thenReturn(sampleList);

        listService.unarchiveList(1L, 1L);

        assertThat(sampleList.isArchived()).isFalse();
        assertThat(sampleList.getPosition()).isEqualTo(4);
    }

    @Test
    @DisplayName("unarchiveList should throw 400 when list is not archived")
    void unarchiveList_notArchived_throws() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));

        assertThatThrownBy(() -> listService.unarchiveList(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("getListsByBoard should return mapped list responses")
    void getListsByBoard_success() {
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(sampleList));

        List<ListResponse> result = listService.getListsByBoard(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoardId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getArchivedLists should return archived lists")
    void getArchivedLists_success() {
        sampleList.setArchived(true);
        when(listRepository.findByBoardIdAndIsArchivedTrue(10L))
                .thenReturn(List.of(sampleList));

        List<ListResponse> result = listService.getArchivedLists(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isArchived()).isTrue();
    }

    @Test
    @DisplayName("updateList should update name and color")
    void updateList_success() {
        UpdateListRequest req = new UpdateListRequest();
        req.setName("Updated");
        req.setColor("#FFF");

        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));
        when(listRepository.save(any())).thenReturn(sampleList);

        ListResponse response = listService.updateList(1L, req, 1L);

        assertThat(response.getName()).isEqualTo("Updated");
        assertThat(response.getColor()).isEqualTo("#FFF");
    }

    @Test
    @DisplayName("updateList should throw 400 when list is archived")
    void updateList_archived_throws() {
        sampleList.setArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));

        UpdateListRequest req = new UpdateListRequest();

        assertThatThrownBy(() -> listService.updateList(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("reorderLists should assign new positions and save")
    void reorderLists_success() {
        TaskList list2 = TaskList.builder().id(2L).boardId(10L).position(1).build();
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(sampleList, list2));

        ReorderListRequest req = new ReorderListRequest();
        req.setBoardId(10L);
        req.setOrderedListIds(List.of(2L, 1L));

        listService.reorderLists(req, 1L);

        verify(listRepository, times(2)).save(any());
        assertThat(list2.getPosition()).isEqualTo(0);
        assertThat(sampleList.getPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("reorderLists should throw 400 if list does not belong to board")
    void reorderLists_invalidListId_throws() {
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(sampleList));

        ReorderListRequest req = new ReorderListRequest();
        req.setBoardId(10L);
        req.setOrderedListIds(List.of(999L));

        assertThatThrownBy(() -> listService.reorderLists(req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("moveList should update boardId and position")
    void moveList_success() {
        MoveListRequest req = new MoveListRequest();
        req.setTargetBoardId(20L);
        req.setTargetPosition(0);

        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));
        when(listRepository.save(any())).thenReturn(sampleList);

        ListResponse response = listService.moveList(1L, req, 1L);

        verify(listRepository).shiftPositionsLeft(10L, 0);
        verify(listRepository).shiftPositionsRight(20L, 0);
        assertThat(response.getBoardId()).isEqualTo(20L);
        assertThat(response.getPosition()).isEqualTo(0);
    }

    @Test
    @DisplayName("moveList should throw 400 if target board is same as source")
    void moveList_sameBoard_throws() {
        MoveListRequest req = new MoveListRequest();
        req.setTargetBoardId(10L); // same as sampleList.getBoardId()

        when(listRepository.findById(1L)).thenReturn(Optional.of(sampleList));

        assertThatThrownBy(() -> listService.moveList(1L, req, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}