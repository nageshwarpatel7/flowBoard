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
}