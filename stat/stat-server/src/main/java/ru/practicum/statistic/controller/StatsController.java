package ru.practicum.statistic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statistic.dto.EndpointHitRequest;
import ru.practicum.statistic.dto.ViewStats;
import ru.practicum.statistic.model.EndpointHit;
import ru.practicum.statistic.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHit saveHit(@Valid @RequestBody EndpointHitRequest hitRequest) {
        log.info("Получен запрос на сохранение информации о обращении: app={}, uri={}, ip={}",
                hitRequest.getApp(), hitRequest.getUri(), hitRequest.getIp());
        return statsService.saveHit(hitRequest);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("Получен запрос статистики с {} по {}, uris: {}, unique: {}", start, end, uris, unique);
        return statsService.getStats(start, end, uris, unique);
    }
}