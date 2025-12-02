package ru.practicum.request.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId, HttpServletRequest request) {
        return requestService.getUserRequests(userId, request);
    }


    @PostMapping("/{userId}//requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(@PathVariable Long userId,
                                                           @RequestParam Long eventId) {
        return requestService.addParticipationRequest(userId, eventId);
    }


    @PatchMapping("/{userId}//requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }


    @GetMapping("/{userId}//events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId, @PathVariable Long eventId,
                                                              HttpServletRequest request) {
        return requestService.getEventParticipants(userId, eventId, request);
    }


    @PatchMapping("/{userId}//events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest eventStatusUpdate,
                                                              HttpServletRequest request) {
        return requestService.changeRequestStatus(userId, eventId, eventStatusUpdate, request);
    }

}