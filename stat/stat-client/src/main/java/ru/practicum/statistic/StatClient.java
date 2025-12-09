package ru.practicum.statistic;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final String appName;

    public StatClient(@Value("${stat.server.url}") String serverUrl,
                      @Value("${spring.application.name}") String appName) {
        log.info("StatClient baseUrl = {}", serverUrl);
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
        this.appName = appName;
    }

    public void hit(HttpServletRequest request) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(appName)
                .uri(request.getRequestURI())
                .ip(extractClientIp(request))
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();

        try {
            restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ResponseStatusException(
                                res.getStatusCode(),
                                res.getBody().toString()
                        );
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ResponseStatusException(
                                res.getStatusCode(),
                                res.getBody().toString()
                        );
                    })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Не удалось отправить hit в сервис статистики", e);
        }
    }

    public List<ViewStatsDto> getStatistics(LocalDateTime start,
                                            LocalDateTime end,
                                            List<String> uris,
                                            Boolean unique) {

        String startStr = start.format(FORMATTER);
        String endStr = end.format(FORMATTER);

        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/stats")
                                .queryParam("start", startStr)
                                .queryParam("end", endStr)
                                .queryParam("unique", unique);

                        if (uris != null && !uris.isEmpty()) {
                            for (String uri : uris) {
                                builder.queryParam("uris", uri);
                            }
                        }

                        var uri = builder.build();
                        log.info("Request stats URI: {}", uri.toString());
                        return uri;
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ResponseStatusException(
                                res.getStatusCode(),
                                res.getBody().toString()
                        );
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ResponseStatusException(
                                res.getStatusCode(),
                                res.getBody().toString()
                        );
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("Не удалось получить статистику из сервиса статистики", e);
            return Collections.emptyList();
        }
    }


    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
