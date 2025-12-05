package ru.practicum.request.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.*;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId, HttpServletRequest request) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException(String.format("User with id %s not found",
                userId)));
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        if (eventRepository.findByIdAndInitiator_Id(eventId, userId).isPresent()) {
            throw new InitiatorRequestException(String.format("User with id %s is initiator for event with id %s",
                    userId, eventId));
        }
        if (!requestRepository.findByRequesterIdAndEventId(userId, eventId).isEmpty()) {
            throw new RepeatableUserRequestException(String.format("User with id %s already make request for event with id %s",
                    userId, eventId));
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(String.format(
                "Event with id %s not found", eventId)));
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotPublishEventException(String.format("Event with id %s is not published", eventId));
        }
        Request request = new Request();
        request.setRequester(userRepository.findById(userId).get());
        request.setEvent(event);

        Long confirmedRequestsAmount = requestRepository.countRequestsByEventAndStatus(event, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() <= confirmedRequestsAmount && event.getParticipantLimit() != 0) {
            throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
        }

        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            request.setCreatedOn(LocalDateTime.now());
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        }

        if (event.getRequestModeration()) {
            request.setStatus(RequestStatus.PENDING);
            request.setCreatedOn(LocalDateTime.now());
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
            request.setCreatedOn(LocalDateTime.now());
        }
        return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request cancellingRequest = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id %s not found or unavailable " +
                        "for user with id %s", requestId, userId)));
        cancellingRequest.setStatus(RequestStatus.CANCELED);
        cancellingRequest = requestRepository.save(cancellingRequest);
        return requestMapper.requestToParticipationRequestDto(cancellingRequest);
    }

}