package ru.practicum.statistic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StatClient {
    private static final String SCHEME = "http";
    private final RestClient restClient;
    private final String serverUrl;


    public StatClient(@Value("${stat.server.url}") String serverUrl) {
        this.restClient = RestClient.create();
        this.serverUrl = serverUrl;
    }

    public void hit(EndpointHitDto dto) {
        String uri = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(serverUrl)
                .port(9090)
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

    public List<ViewStatsDto> getStatistics(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String uriWithParams = UriComponentsBuilder.newInstance()
                .scheme(SCHEME)
                .host(serverUrl)
                .port(9090)
                .path("/stats")
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .queryParam("start", start)
                .queryParam("end", end)
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
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
