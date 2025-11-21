package ru.practicum.statistic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.statistic.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query(value = "SELECT app, uri, COUNT(id) as hits " +
            "FROM endpoint_hits " +
            "WHERE hit_timestamp BETWEEN ?1 AND ?2 " +
            "AND (?3 IS NULL OR uri IN (?3)) " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<StatsProjection> findStatsNative(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query(value = "SELECT app, uri, COUNT(DISTINCT ip) as hits " +
            "FROM endpoint_hits " +
            "WHERE hit_timestamp BETWEEN ?1 AND ?2 " +
            "AND (?3 IS NULL OR uri IN (?3)) " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<StatsProjection> findUniqueStatsNative(LocalDateTime start, LocalDateTime end, List<String> uris);

    interface StatsProjection {
        String getApp();

        String getUri();

        Long getHits();
    }
}