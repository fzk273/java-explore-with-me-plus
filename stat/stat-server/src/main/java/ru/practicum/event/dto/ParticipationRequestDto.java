package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("requester")
    private Long requester;

    @JsonProperty("event")
    private Long event;

    @JsonProperty("status")
    private ru.practicum.event.model.ParticipationRequest.ParticipationRequestStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created")
    private LocalDateTime created;
}