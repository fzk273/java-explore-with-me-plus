package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.ParticipationRequestRepository;
import ru.practicum.user.dto.UserShortDto;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventMapper {

    private final ParticipationRequestRepository requestRepository;

    public EventFullDto toFullDto(Event event, CategoryDto category, UserShortDto initiator) {
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(category)
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .description(event.getDescription())
                .participantLimit(event.getParticipantLimit())
                .state(event.getState())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .location(EventFullDto.LocationDto.builder()
                        .lat(event.getLat())
                        .lon(event.getLon())
                        .build())
                .requestModeration(event.getRequestModeration())
                .confirmedRequests(confirmedRequests)
                .views(event.getViews() != null ? event.getViews() : 0L)
                .build();
    }

    public EventShortDto toShortDto(Event event) {
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .confirmedRequests(confirmedRequests)
                .views(event.getViews() != null ? event.getViews() : 0L)
                .build();
    }
}