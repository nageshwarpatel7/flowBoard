package com.flowboard.card_service.service;

import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.entity.CardActivity;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.repository.CardActivityRepository;
import com.flowboard.card_service.repository.CardRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardServiceImpl Unit Tests")
class CardServiceImplTest {

    @Mock CardRepository cardRepository;
    @Mock CardActivityRepository activityRepository;

    @InjectMocks CardServiceImpl cardService;

    private Card sampleCard;

    @BeforeEach
    void setUp() {
        sampleCard = Card.builder()
                .id(1L).listId(10L).boardId(100L)
                .title("Design login page")
                .position(0).priority(Priority.MEDIUM)
                .status(CardStatus.TO_DO).isArchived(false)
                .createdById(1L)
                .createdAt(LocalDateTime.now()).build();
    }

    // ── createCard ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCard()")
    class CreateCardTests {

        @Test
        @DisplayName("should append at end when position is null")
        void createCard_appendsAtEnd() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L);
            req.setTitle("New Card"); req.setPosition(null);

            when(cardRepository.findMaxPositionByListId(10L))
                    .thenReturn(Optional.of(2));
            when(cardRepository.save(any())).thenReturn(sampleCard);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            CardResponse response = cardService.createCard(req, 1L);

            assertThat(response).isNotNull();
            verify(cardRepository, never()).shiftPositionsRight(any(), any());
        }

        @Test
        @DisplayName("should insert at position and shift siblings right")
        void createCard_insertsAtPosition() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L);
            req.setTitle("Inserted Card"); req.setPosition(1);

            when(cardRepository.save(any())).thenReturn(sampleCard);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.createCard(req, 1L);

            verify(cardRepository).shiftPositionsRight(10L, 1);
        }

        @Test
        @DisplayName("should get position 0 when list is empty")
        void createCard_firstInList_getsPosition0() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(10L); req.setBoardId(100L);
            req.setTitle("First Card");

            when(cardRepository.findMaxPositionByListId(10L))
                    .thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                assertThat(c.getPosition()).isEqualTo(0);
                return c;
            });
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.createCard(req, 1L);
        }
    }

    // ── archiveCard ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("archiveCard()")
    class ArchiveCardTests {

        @Test
        @DisplayName("should archive card and shift siblings left")
        void archiveCard_success() {
            when(cardRepository.findById(1L))
                    .thenReturn(Optional.of(sampleCard));
            when(cardRepository.save(any())).thenReturn(sampleCard);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            CardResponse response = cardService.archiveCard(1L, 1L);

            assertThat(response).isNotNull();
            verify(cardRepository).shiftPositionsLeft(sampleCard.getListId(),
                    sampleCard.getPosition());
        }

        @Test
        @DisplayName("should throw 400 when card already archived")
        void archiveCard_alreadyArchived_throws() {
            sampleCard.setArchived(true);
            when(cardRepository.findById(1L))
                    .thenReturn(Optional.of(sampleCard));

            assertThatThrownBy(() -> cardService.archiveCard(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── setPriority ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setPriority()")
    class SetPriorityTests {

        @Test
        @DisplayName("should update priority and log activity")
        void setPriority_success() {
            SetPriorityRequest req = new SetPriorityRequest();
            req.setPriority(Priority.HIGH);

            when(cardRepository.findById(1L))
                    .thenReturn(Optional.of(sampleCard));
            when(cardRepository.save(any())).thenReturn(sampleCard);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            CardResponse response = cardService.setPriority(1L, req, 1L);

            assertThat(sampleCard.getPriority()).isEqualTo(Priority.HIGH);
            verify(activityRepository).save(argThat(a ->
                    a.getActionType().equals("PRIORITY_CHANGE")));
        }
    }

    // ── setStatus ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setStatus()")
    class SetStatusTests {

        @Test
        @DisplayName("should update status to IN_PROGRESS")
        void setStatus_success() {
            SetStatusRequest req = new SetStatusRequest();
            req.setStatus(CardStatus.IN_PROGRESS);

            when(cardRepository.findById(1L))
                    .thenReturn(Optional.of(sampleCard));
            when(cardRepository.save(any())).thenReturn(sampleCard);
            when(activityRepository.save(any())).thenReturn(new CardActivity());

            cardService.setStatus(1L, req, 1L);

            assertThat(sampleCard.getStatus()).isEqualTo(CardStatus.IN_PROGRESS);
            verify(activityRepository).save(argThat(a ->
                    a.getActionType().equals("STATUS_CHANGE") &&
                            a.getOldValue().equals("TO_DO") &&
                            a.getNewValue().equals("IN_PROGRESS")
            ));
        }
    }

    // ── deleteCard ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCard should shift siblings left and delete")
    void deleteCard_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));

        cardService.deleteCard(1L, 1L);

        verify(cardRepository).shiftPositionsLeft(
                sampleCard.getListId(), sampleCard.getPosition());
        verify(cardRepository).delete(sampleCard);
    }

    // ── getCardById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCardById should throw 404 when card not found")
    void getCardById_notFound_throws() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── moveCard ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("moveCard should update listId and close gap in source list")
    void moveCard_success() {
        MoveCardRequest req = new MoveCardRequest();
        req.setTargetListId(20L);
        req.setTargetBoardId(100L);
        req.setTargetPosition(null);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.findMaxPositionByListId(20L))
                .thenReturn(Optional.of(3));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.moveCard(1L, req, 1L);

        verify(cardRepository).shiftPositionsLeft(10L, sampleCard.getPosition());
        assertThat(sampleCard.getListId()).isEqualTo(20L);
        assertThat(sampleCard.getPosition()).isEqualTo(0);
    }
}