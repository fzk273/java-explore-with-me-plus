package ru.practicum.event.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;
import ru.practicum.event.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class EventPrivateController {

    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST /users/{}/events - создание события", userId);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> getEventsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("GET /users/{}/events?from={}&size={} - получение событий пользователя", userId, from, size);
        return eventService.getEventsByUser(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByUser(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - получение события пользователя", userId, eventId);
        return eventService.getEventByUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByUser(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest updateEvent) {
        log.info("PATCH /users/{}/events/{} - обновление события пользователя", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, updateEvent);
    }

    @PatchMapping("/{eventId}/cancel")
    public EventFullDto cancelEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("PATCH /users/{}/events/{}/cancel - отмена события", userId, eventId);
        return eventService.cancelEventByUser(userId, eventId);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsForEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение запросов на событие", userId, eventId);
        return requestService.getRequestsForEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public RequestStatusUpdateResult updateRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody RequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests - обновление статусов запросов", userId, eventId);
        return requestService.updateRequestsStatus(userId, eventId, request);
    }
}