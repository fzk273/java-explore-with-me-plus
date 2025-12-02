package ru.practicum.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class Location {
    private Float lat;
    private Float lon;
}