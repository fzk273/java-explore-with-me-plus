package ru.practicum.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@Component
public class EventMapper {
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public EventMapper(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }


    public Event mapToEvent(NewEventDto newEventDto, User user, Category category) {
        Event event = new Event();

        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiator(user);
        event.setCategory(category);
        event.setConfirmedRequests(0L);
        event.setLat(newEventDto.getLocation().getLat());
        event.setLon(newEventDto.getLocation().getLon());
        event.setPaid(newEventDto.getPaid());

        Long participantLimit = newEventDto.getParticipantLimit() == null ? 0L : newEventDto.getParticipantLimit();
        event.setParticipantLimit(participantLimit);

        event.setRequestModeration(newEventDto.getRequestModeration());
        event.setState(EventState.PENDING);
        event.setTitle(newEventDto.getTitle());
        event.setViews(0L);

        return event;
    }

    public EventFullDto mapToFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto fullDto = new EventFullDto();

        fullDto.setId(event.getId());
        fullDto.setTitle(event.getTitle());
        fullDto.setAnnotation(event.getAnnotation());
        fullDto.setDescription(event.getDescription());
        fullDto.setEventDate(event.getEventDate());
        fullDto.setCreatedOn(event.getCreatedOn());

        fullDto.setCategory(event.getCategory() != null ?
                categoryMapper.toCategoryDto(event.getCategory()) :
                new CategoryDto());

        fullDto.setInitiator(event.getInitiator() != null ?
                userMapper.toUserShortDto(event.getInitiator()) :
                new UserShortDto());

        Location location = null;
        if (event.getLat() != null && event.getLon() != null) {
            location = new Location(event.getLat(), event.getLon());
        }
        fullDto.setLocation(location);

        fullDto.setPaid(event.getPaid() != null ? event.getPaid() : Boolean.FALSE);
        fullDto.setParticipantLimit(event.getParticipantLimit() != null ? event.getParticipantLimit() : 0L);
        fullDto.setRequestModeration(event.getRequestModeration() != null ? event.getRequestModeration() : Boolean.TRUE);
        fullDto.setPublishedOn(event.getPublishedOn());

        fullDto.setState(event.getState() != null ? event.getState() : EventState.PENDING);

        // Числовые поля - никогда не должны быть null для JSON
        fullDto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0L);
        fullDto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return fullDto;
    }

    public EventShortDto mapToShortDto(Event event) {
        EventShortDto shortDto = new EventShortDto();

        shortDto.setAnnotation(event.getAnnotation());
        shortDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
        shortDto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0L);
        shortDto.setEventDate(event.getEventDate());
        shortDto.setId(event.getId());
        shortDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));
        shortDto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        shortDto.setTitle(event.getTitle());
        shortDto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return shortDto;
    }

    public void updateEventFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {
            event.setLat(request.getLocation().getLat());
            event.setLon(request.getLocation().getLon());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    public void updateEventFromUserRequest(UpdateEventUserRequest request, Event event) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {
            event.setLat(request.getLocation().getLat());
            event.setLon(request.getLocation().getLon());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }
}