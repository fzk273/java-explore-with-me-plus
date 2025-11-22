package ru.practicum.statistic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statistic.dto.EndpointHitRequest;
import ru.practicum.statistic.dto.ViewStats;
import ru.practicum.statistic.model.EndpointHit;
import ru.practicum.statistic.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;

    @Transactional
    public EndpointHit saveHit(EndpointHitRequest hitRequest) {
        EndpointHit hit = EndpointHit.builder()
                .app(hitRequest.getApp())
                .uri(hitRequest.getUri())
                .ip(hitRequest.getIp())
                .timestamp(hitRequest.getTimestamp())
                .build();
        return statsRepository.save(hit);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {

        List<StatsRepository.StatsProjection> results;

        if (Boolean.TRUE.equals(unique)) {
            results = statsRepository.findUniqueStatsNative(start, end, uris);
        } else {
            results = statsRepository.findStatsNative(start, end, uris);
        }

        return results.stream()
                .map(p -> new ViewStats(p.getApp(), p.getUri(), p.getHits()))
                .toList();
    }
}