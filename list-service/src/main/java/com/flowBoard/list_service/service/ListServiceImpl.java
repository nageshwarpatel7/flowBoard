package com.flowBoard.list_service.service;

import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.entity.TaskList;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.repository.ListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListServiceImpl implements ListService{

    public final ListRepository listRepository;

    @Override
    @Transactional
    public ListResponse createList(CreateListRequest request, Long userId){
        int position;

        if(request.getPosition()!=null){
            listRepository.shiftPositionsRight( request.getBoardId(), request.getPosition());
            position = request.getPosition();
        }else{
            position = listRepository
                    .findMaxPositionByBoardId(request.getBoardId())
                    .map(max->max+1)
                    .orElse(0);
        }

        TaskList list = TaskList.builder()
                .boardId(request.getBoardId())
                .name(request.getName())
                .position(position)
                .color(request.getColor())
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        listRepository.save(list);
        log.info("List created: id={} name={} boardId={} position={}", list.getId(),
                list.getName(), list.getBoardId(), list.getPosition());

        return toResponse(list);
    }

    @Override
    public ListResponse getListById(Long listId) {
        return toResponse(findList(listId));
    }

    @Override
    public List<ListResponse> getListsByBoard(Long boardId){
        return listRepository
                .findByBoardIdAndIsArchivedFalseOrderByPosition(boardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ListResponse updateList(Long listId, UpdateListRequest request,
                                   Long userId){
        TaskList list = findList(listId);

        if(list.isArchived()){
            throw new CustomException(
                    "Cannot update an archived list - unarchive if first",
                    HttpStatus.BAD_REQUEST
            );
        }

        list.setName(request.getName());
        if(request.getColor()!=null){
            list.setColor(request.getColor());
        }

        list.setUpdatedAt(LocalDateTime.now());
        listRepository.save(list);
        log.info("List updated: id={} name={}", listId, request.getName());
        return toResponse(list);
    }



    @Override
    @Transactional
    public void deleteList(Long listId, Long userId){
        TaskList list = findList(listId);

        if(!list.isArchived()){
            listRepository.shiftPositionsLeft(list.getBoardId(), list.getPosition());
        }

        listRepository.delete((list));
        log.info("List deleted: id={} boardId={}", listId, list.getBoardId());
    }

    @Override
    @Transactional
    public List<ListResponse> reorderLists(ReorderListRequest request, Long userId){

        List<Long> orderedIds = request.getOrderedListIds();

        List<TaskList> allLists = listRepository
                .findByBoardIdAndIsArchivedFalseOrderByPosition(request.getBoardId());

        List<Long> existingIds = allLists.stream()
                .map(TaskList::getId)
                .toList();

        for(Long id : orderedIds){
            if(!existingIds.contains(id)){
                throw new CustomException(
                        "List Id="+id+" does not belong to board id="
                        +request.getBoardId(), HttpStatus.BAD_REQUEST
                );
            }
        }

        AtomicInteger pos = new AtomicInteger(0);
        orderedIds.forEach(id-> {
            TaskList list = allLists.stream().filter(l-> l.getId().equals(id))
                    .findFirst().orElseThrow();
            list.setPosition(pos.getAndIncrement());
            list.setUpdatedAt(LocalDateTime.now());
            listRepository.save(list);
        });

        log.info("Lists reordered for boardId={}", request.getBoardId());

        return listRepository
                .findByBoardIdAndIsArchivedFalseOrderByPosition(request.getBoardId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ListResponse archiveList(Long listId, Long userId){
        TaskList list = findList(listId);

        if(list.isArchived()){
            throw new CustomException("List is already archived", HttpStatus.BAD_REQUEST);
        }

        listRepository.shiftPositionsLeft(list.getBoardId(), list.getPosition());

        list.setArchived(true);
        list.setUpdatedAt(LocalDateTime.now());
        listRepository.save(list);

        log.info("List archived: id={} boardId={}", listId, list.getBoardId());
        return toResponse(list);
    }

    @Override
    @Transactional
    public ListResponse unarchiveList(Long listId, Long userId){
        TaskList list = findList(listId);

        if(!list.isArchived()){
            throw new CustomException("List is not archived", HttpStatus.BAD_REQUEST);
        }

        int newPosition = listRepository.findMaxPositionByBoardId(list.getBoardId())
                .map(max->max+1).orElse(0);
        list.setArchived(false);
        list.setPosition(newPosition);
        list.setUpdatedAt(LocalDateTime.now());
        listRepository.save(list);

        log.info("List unarchived: id={} newPosition={}", listId, newPosition);
        return toResponse(list);
    }


    @Override
    public List<ListResponse> getArchivedLists(Long boardId) {
        return listRepository
                .findByBoardIdAndIsArchivedTrue(boardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ListResponse moveList(Long listId, MoveListRequest request,
                                 Long userId){
        TaskList list = findList(listId);
        Long sourceBoardId = list.getBoardId();
        Long targetBoardId = request.getTargetBoardId();

        if(sourceBoardId.equals(targetBoardId)){
            throw new CustomException(
                    "List is already on the target board", HttpStatus.BAD_REQUEST
            );
        }

        if(!list.isArchived()){
            listRepository.shiftPositionsLeft(sourceBoardId, list.getPosition());
        }

        int targetPosition;
        if(request.getTargetPosition()!=null){
            listRepository.shiftPositionsRight(
                    targetBoardId, request.getTargetPosition());
            targetPosition = request.getTargetPosition();
        }
        else{
            targetPosition = listRepository
                    .findMaxPositionByBoardId(targetBoardId)
                    .map(max->max+1)
                    .orElse(0);
        }

        list.setBoardId(targetBoardId);
        list.setPosition(targetPosition);
        list.setArchived(false);
        list.setUpdatedAt(LocalDateTime.now());
        listRepository.save(list);

        log.info("List moved: id={} from boardId={} to boardId={} position={}",
                listId, sourceBoardId, targetBoardId, targetPosition);

        return toResponse(list);
    }


    private TaskList findList(Long listId){
        return listRepository.findById(listId)
                .orElseThrow(()-> new CustomException("List not found", HttpStatus.NOT_FOUND));
    }

    private ListResponse toResponse(TaskList list){
        return ListResponse.builder()
                .id(list.getId())
                .boardId(list.getBoardId())
                .name(list.getName())
                .position(list.getPosition())
                .color(list.getColor())
                .isArchived(list.isArchived())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .cardCount(0)
                .build();
    }
}
