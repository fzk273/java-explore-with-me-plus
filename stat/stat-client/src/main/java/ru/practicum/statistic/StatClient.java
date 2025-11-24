package ru.practicum.statistic;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatClient {

    private static final String SCHEME = "http";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final String serverUrl;
    private final String appName;
    private final Integer port;

    public StatClient(@Value("${stat.server.url}") String serverUrl,
                      @Value("${spring.application.name}") String appName,
                      @Value("${stat.server.port}") Integer port) {
        this.port = port;
        this.restClient = RestClient.create();
        this.serverUrl = serverUrl;
        this.appName = appName;
    }

    public void hit(HttpServletRequest request) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(appName)
                .uri(request.getRequestURI())
                .ip(extractClientIp(request))
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();

        sendHit(dto);
    }

    private void sendHit(EndpointHitDto dto) {
        String uri = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(serverUrl)
                .port(port)
                .path("/hit")
                .toUriString();

        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            response.getBody().toString()
                    );
                })
                .toBodilessEntity();
    }

    public List<ViewStatsDto> getStatistics(LocalDateTime start,
                                            LocalDateTime end,
                                            List<String> uris,
                                            Boolean unique) {


        String startStr = encodeDate(start);
        String endStr = encodeDate(end);
        String urisCsv = String.join(",", uris);

        String uriWithParams = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(serverUrl)
                .port(port)
                .path("/stats")
                .queryParam("uris", urisCsv)
                .queryParam("unique", unique)
                .queryParam("start", startStr)
                .queryParam("end", endStr)
                .toUriString();

        return restClient.get()
                .uri(uriWithParams)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            response.getBody().toString()
                    );
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    private String encodeDate(LocalDateTime date) {
        return URLEncoder.encode(date.format(FORMATTER), StandardCharsets.UTF_8);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
