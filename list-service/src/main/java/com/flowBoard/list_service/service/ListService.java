package com.flowBoard.list_service.service;

import com.flowBoard.list_service.dto.*;

import java.util.List;

public interface ListService {

    //CRUD
    ListResponse createList(CreateListRequest request, Long userId);
    ListResponse getListById(Long listId);
    List<ListResponse> getListsByBoard(Long boardId);
    ListResponse updateList(Long listId, UpdateListRequest request, Long userId);
    void deleteList(Long listId, Long userId);

    List<ListResponse> reorderLists(ReorderListRequest request, Long userId);

    ListResponse archiveList(Long listId, Long userId);
    ListResponse unarchiveList(Long listId, Long userId);
    List<ListResponse> getArchivedLists(Long boardId);

    ListResponse moveList(Long listId, MoveListRequest request, Long userId);
}
