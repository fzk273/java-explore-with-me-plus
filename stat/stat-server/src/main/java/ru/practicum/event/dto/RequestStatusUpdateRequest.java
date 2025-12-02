package ru.practicum.event.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatusUpdateRequest {
    private List<Long> requestIds;
    private Status status;

    public enum Status {
        CONFIRMED,
        REJECTED
    }
}