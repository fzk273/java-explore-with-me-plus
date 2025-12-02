package ru.practicum.event.service;

import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.dto.RequestStatusUpdateRequest;
import ru.practicum.event.dto.RequestStatusUpdateResult;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId);

    ParticipationRequestDto confirmRequest(Long userId, Long eventId, Long reqId);

    ParticipationRequestDto rejectRequest(Long userId, Long eventId, Long reqId);

    List<ParticipationRequestDto> processRequestsStatus(Long userId, Long eventId, List<Long> requestIds, String status);

    RequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, RequestStatusUpdateRequest request);
}