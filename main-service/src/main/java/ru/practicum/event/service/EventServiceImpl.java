package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.statistic.StatClient;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final StatClient statClient;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id = " + newEventDto.getCategory() + " не найдена"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.mapToEvent(newEventDto, user, category);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.mapToFullDto(savedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        if (updateRequest.getParticipantLimit() != null && updateRequest.getParticipantLimit() < 0) {
            throw new BadRequestException("Лимит участников не может быть отрицательным");
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromUserRequest(updateRequest, event);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);

        return eventMapper.mapToFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата начала события должна быть не ранее чем за час от даты публикации");
        }

        if (updateRequest.getParticipantLimit() != null && updateRequest.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromAdminRequest(updateRequest, event);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания публикации");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Событие можно отклонить только если оно еще не опубликовано");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);

        return eventMapper.mapToFullDto(updatedEvent);
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        List<Event> events = Collections.singletonList(event);
        Long eventViews = getEventsViews(events).getOrDefault(eventId, 0L);
        EventFullDto eventFullDto = eventMapper.mapToFullDto(event);
        eventFullDto.setViews(eventViews);

        return eventFullDto;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        List<Event> events = Collections.singletonList(event);
        Long eventViews = getEventsViews(events).getOrDefault(eventId, 0L);
        EventFullDto eventFullDto = eventMapper.mapToFullDto(event);
        eventFullDto.setViews(eventViews);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        Map<Long, Long> viewsMap = getEventsViews(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(eventMapper::mapToShortDto)
                .toList();

        eventShortDtos.forEach(
                eventShortDto -> eventShortDto.setViews(viewsMap.getOrDefault(eventShortDto.getId(), 0L))
        );

        return eventShortDtos;
    }

    @Override
    public List<EventFullDto> searchForAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        List<Event> allEvents = eventRepository.findAll();

        List<Event> filteredEvents = allEvents.stream()
                .filter(event -> users == null || users.isEmpty() ||
                        users.contains(event.getInitiator().getId()))
                .filter(event -> states == null || states.isEmpty() ||
                        states.contains(event.getState()))
                .filter(event -> categories == null || categories.isEmpty() ||
                        categories.contains(event.getCategory().getId()))
                .filter(event -> rangeStart == null ||
                        !event.getEventDate().isBefore(rangeStart))
                .filter(event -> rangeEnd == null ||
                        !event.getEventDate().isAfter(rangeEnd))
                .toList();

        int startIndex = Math.min(from, filteredEvents.size());
        int endIndex = Math.min(from + size, filteredEvents.size());

        if (startIndex >= endIndex) {
            return Collections.emptyList();
        }

        List<Event> paginatedEvents = filteredEvents.subList(startIndex, endIndex);
        Map<Long, Long> viewsMap = getEventsViews(paginatedEvents);
        List<EventFullDto> result = new ArrayList<>();

        for (Event event : paginatedEvents) {
            EventFullDto eventFullDto = eventMapper.mapToFullDto(event);
            eventFullDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
            result.add(eventFullDto);
        }

        return result;
    }

//    @Override
//    public List<EventFullDto> searchForAdmin(List<Long> users, List<EventState> states, List<Long> categories,
//                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
//        log.info("Поиск событий администратором: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
//                users, states, categories, rangeStart, rangeEnd, from, size);
//
//        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
//
//        List<EventState> eventStates = null;
//
//        if (states != null && !states.isEmpty()) {
//            eventStates = new ArrayList<>(states);
//        }
//
//        List<Event> events = eventRepository.findEventsWithFilters(users, eventStates, categories, rangeStart, rangeEnd, pageable);
//
//        return events.stream()
//                .map(eventMapper::mapToFullDto)
//                .collect(Collectors.toList());
//    }

    @Override
    public List<EventShortDto> searchForUser(String text, List<Long> categories, Boolean paid,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Boolean onlyAvailable, String sort, Integer from, Integer size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        LocalDateTime actualRangeStart = (rangeStart != null) ? rangeStart : LocalDateTime.now();

        List<Event> allEvents = eventRepository.findAll();

        List<Event> filteredEvents = allEvents.stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .filter(event -> text == null || text.isEmpty() ||
                        event.getAnnotation().toLowerCase().contains(text.toLowerCase()) ||
                        event.getDescription().toLowerCase().contains(text.toLowerCase()))
                .filter(event -> categories == null || categories.isEmpty() ||
                        categories.contains(event.getCategory().getId()))
                .filter(event -> {
                    if (paid == null) return true;
                    Boolean eventPaid = event.getPaid();
                    return eventPaid != null && eventPaid.equals(paid);
                })
                .filter(event -> !event.getEventDate().isBefore(actualRangeStart))
                .filter(event -> rangeEnd == null || !event.getEventDate().isAfter(rangeEnd))
                .filter(event -> {
                    if (onlyAvailable == null || !onlyAvailable) {
                        return true;
                    }
                    Long limit = event.getParticipantLimit();
                    Long confirmed = event.getConfirmedRequests();
                    if (limit == null || limit == 0) {
                        return true;
                    }
                    if (confirmed == null) {
                        confirmed = 0L;
                    }
                    return confirmed < limit;
                })
                .toList();

        int startIndex = Math.min(from, filteredEvents.size());
        int endIndex = Math.min(from + size, filteredEvents.size());

        if (startIndex >= endIndex) {
            return Collections.emptyList();
        }

        List<Event> paginatedEvents = filteredEvents.subList(startIndex, endIndex);
        Map<Long, Long> viewsMap = getEventsViews(paginatedEvents);
        List<EventShortDto> result = new ArrayList<>();

        for (Event event : paginatedEvents) {
            EventShortDto shortDto = eventMapper.mapToShortDto(event);
            shortDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
            result.add(shortDto);
        }

        if ("VIEWS".equalsIgnoreCase(sort)) {
            result.sort((e1, e2) -> Long.compare(
                    e2.getViews() != null ? e2.getViews() : 0L,
                    e1.getViews() != null ? e1.getViews() : 0L
            ));
        } else if ("EVENT_DATE".equalsIgnoreCase(sort) || sort == null) {
            result.sort((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()));
        }

        return result;
    }

//    @Override
//    public List<EventShortDto> searchForUser(String text, List<Long> categories, Boolean paid,
//                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
//                                               String sort, Integer from, Integer size) {
//        log.info("Публичный поиск событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
//                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
//
//        // Валидация дат
//        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
//            throw new BadRequestException("End date must be after start date");
//        }
//
//        Pageable pageable;
//        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
//            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
//        } else if ("VIEWS".equalsIgnoreCase(sort)) {
//            pageable = PageRequest.of(from / size, size, Sort.by("views").descending());
//        } else {
//            pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
//        }
//
//        if (rangeStart == null) {
//            rangeStart = LocalDateTime.now();
//        }
//
//        List<Event> events = eventRepository.findPublishedEventsWithFilters(
//                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);
//
//        return events.stream()
//                .map(eventMapper::mapToShortDto)
//                .collect(Collectors.toList());
//    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id = " + userId + " не является инициатором события с id = " + eventId
            );
        }

        return requestRepository.findByEventId(eventId).stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(Long userId, Long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Пользователь с id = " + userId + " не является инициатором события с id = " + eventId
            );
        }

        List<Request> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());
        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResult(); // или ValidationException
        }

        // Проверяем, что все заявки относятся к этому событию и в PENDING
        for (Request request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ForbiddenException(
                        "Запрос с id = " + request.getId() + " не принадлежит событию с id = " + eventId
                );
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }
        }

        long confirmedCount = requestRepository.countRequestsByEventAndStatus(event, RequestStatus.CONFIRMED);
        long participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0L;

        RequestStatus newStatus = updateRequest.getStatus();

        if (newStatus == RequestStatus.CONFIRMED
                && participantLimit > 0
                && confirmedCount + requests.size() > participantLimit) {

            // это как раз тот случай из теста "лимит уже достигнут"
            throw new ConflictException("Достигнут лимит участников для события");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        List<ParticipationRequestDto> confirmedDtos = new ArrayList<>();
        List<ParticipationRequestDto> rejectedDtos = new ArrayList<>();

        for (Request request : requests) {
            if (newStatus == RequestStatus.CONFIRMED) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedDtos.add(requestMapper.requestToParticipationRequestDto(request));
            } else if (newStatus == RequestStatus.REJECTED) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedDtos.add(requestMapper.requestToParticipationRequestDto(request));
            } else {
                throw new ValidationException("Неизвестный статус: " + newStatus);
            }
        }

        event.setConfirmedRequests(confirmedCount);
        eventRepository.save(event);
        requestRepository.saveAll(requests);

        // если после подтверждения лимит выбит — отклоняем все оставшиеся PENDING
        if (participantLimit > 0 && confirmedCount >= participantLimit) {
            List<Request> pendingRequests =
                    requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (Request pending : pendingRequests) {
                pending.setStatus(RequestStatus.REJECTED);
                rejectedDtos.add(requestMapper.requestToParticipationRequestDto(pending));
            }
            requestRepository.saveAll(pendingRequests);
        }

        result.setConfirmedRequests(confirmedDtos);
        result.setRejectedRequests(rejectedDtos);
        return result;
    }

    private Long getEventViews(Long eventId) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

            if (event.getPublishedOn() == null) {
                return 0L;
            }

            LocalDateTime start = event.getPublishedOn();
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            return statClient.getStatistics(start, end, uris, true)
                    .stream()
                    .findFirst()
                    .map(stats -> stats.getHits() != null ? stats.getHits() : 0L)
                    .orElse(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        try {
            if (events.isEmpty()) {
                return Collections.emptyMap();
            }

            LocalDateTime start = events.stream()
                    .map(Event::getPublishedOn)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusYears(1));

            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            LocalDateTime end = LocalDateTime.now();

            return statClient.getStatistics(start, end, uris, true)
                    .stream()
                    .collect(Collectors.toMap(
                            stats -> extractEventIdFromUri(stats.getUri()),
                            stats -> stats.getHits() != null ? stats.getHits() : 0L,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new ValidationException("Неверный формат URI = " + uri);
        }
    }
}