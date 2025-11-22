package ru.practicum.statistic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.statistic.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query(value = """
            SELECT eh.app AS app, eh.uri AS uri, COUNT(eh.id) AS hits
            FROM endpoint_hits eh
            WHERE eh.hit_timestamp BETWEEN :start AND :end
              AND (:uris IS NULL OR eh.uri IN :uris)
            GROUP BY eh.app, eh.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<StatsProjection> findStatsNative(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query(value = """
            SELECT eh.app AS app, eh.uri AS uri, COUNT(DISTINCT eh.ip) AS hits
            FROM endpoint_hits eh
            WHERE eh.hit_timestamp BETWEEN :start AND :end
              AND (:uris IS NULL OR eh.uri IN :uris)
            GROUP BY eh.app, eh.uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<StatsProjection> findUniqueStatsNative(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("uris") List<String> uris);

    interface StatsProjection {
        String getApp();
        String getUri();
        Long getHits();
    }
}