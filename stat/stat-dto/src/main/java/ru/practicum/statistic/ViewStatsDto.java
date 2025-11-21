package ru.practicum.statistic;

import lombok.Data;

@Data
public class ViewStatsDto {
    private String app;
    private String uri;
    private Long hits;
}
