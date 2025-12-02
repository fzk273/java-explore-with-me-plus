package ru.practicum.event.controller.priv;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class RequestPrivateController {

    private final RequestService requestService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(
            @PathVariable Long userId,
            @RequestParam @Positive Long eventId) {
        log.info("POST /users/{}/requests?eventId={} - создание запроса на участие", userId, eventId);
        return requestService.createRequest(userId, eventId);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequestsByUser(
            @PathVariable Long userId) {
        log.info("GET /users/{}/requests - получение запросов пользователя", userId);
        return requestService.getRequestsByUser(userId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel - отмена запроса", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}