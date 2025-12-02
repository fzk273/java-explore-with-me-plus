package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.dto.RequestStatusUpdateRequest;
import ru.practicum.event.dto.RequestStatusUpdateResult;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.ParticipationRequest;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.ParticipationRequestRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса на участие: userId={}, eventId={}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        // Используем метод с JOIN FETCH вместо обычного findById
        Event event = eventRepository.findByIdWithCategoryAndInitiator(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка: инициатор события не может подать заявку на участие
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в своем событии");
        }

        // Проверка: событие должно быть опубликовано
        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // Проверка: дата события должна быть в будущем
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Нельзя участвовать в событии, которое уже началось");
        }

        // Проверка: повторная заявка
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на участие уже существует");
        }

        // Определение статуса заявки
        ParticipationRequest.ParticipationRequestStatus status;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = ParticipationRequest.ParticipationRequestStatus.CONFIRMED;
        } else {
            status = ParticipationRequest.ParticipationRequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Создан запрос на участие с id={}", savedRequest.getId());

        return toDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса на участие: userId={}, requestId={}", userId, requestId);

        // Этот метод использует findByIdAndRequesterId который уже имеет JOIN FETCH в репозитории
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        // Проверка: можно отменять только PENDING запросы
        if (request.getStatus() != ParticipationRequest.ParticipationRequestStatus.PENDING) {
            throw new ConflictException("Можно отменять только запросы в состоянии PENDING");
        }

        request.setStatus(ParticipationRequest.ParticipationRequestStatus.CANCELED);

        ParticipationRequest updatedRequest = requestRepository.save(request);
        log.info("Запрос с id={} отменен", requestId);

        return toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.info("Получение запросов пользователя с id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Этот метод использует findByRequesterId который уже имеет JOIN FETCH в репозитории
        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {
        log.info("Получение запросов на событие с id={} пользователем с id={}", eventId, userId);

        // Проверка, что событие принадлежит пользователю
        // Используем метод с JOIN FETCH
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или доступ запрещен"));

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto confirmRequest(Long userId, Long eventId, Long reqId) {
        return updateRequestStatus(userId, eventId, reqId, ParticipationRequest.ParticipationRequestStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public ParticipationRequestDto rejectRequest(Long userId, Long eventId, Long reqId) {
        return updateRequestStatus(userId, eventId, reqId, ParticipationRequest.ParticipationRequestStatus.REJECTED);
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> processRequestsStatus(Long userId, Long eventId, List<Long> requestIds, String status) {
        log.info("Обработка статусов запросов: userId={}, eventId={}, requestIds={}, status={}",
                userId, eventId, requestIds, status);

        // Проверка, что событие принадлежит пользователю
        // Используем метод с JOIN FETCH
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или доступ запрещен"));

        List<ParticipationRequest> requests = requestRepository.findAllByEventIdAndIdIn(eventId, requestIds);

        if (requests.isEmpty()) {
            throw new NotFoundException("Запросы не найдены");
        }

        // Проверка лимита участников для подтверждения
        if ("CONFIRMED".equals(status)) {
            Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            long pendingRequests = requests.stream()
                    .filter(r -> r.getStatus() == ParticipationRequest.ParticipationRequestStatus.PENDING)
                    .count();

            if (event.getParticipantLimit() > 0 &&
                    confirmedRequests + pendingRequests > event.getParticipantLimit()) {
                throw new ConflictException("Будет превышен лимит участников");
            }
        }

        // Обновление статусов
        requests.forEach(request -> {
            if ("CONFIRMED".equals(status)) {
                request.setStatus(ParticipationRequest.ParticipationRequestStatus.CONFIRMED);
            } else if ("REJECTED".equals(status)) {
                if (request.getStatus() == ParticipationRequest.ParticipationRequestStatus.CONFIRMED) {
                    throw new ConflictException("Нельзя отклонить уже подтвержденный запрос");
                }
                request.setStatus(ParticipationRequest.ParticipationRequestStatus.REJECTED);
            }
        });

        List<ParticipationRequest> updatedRequests = requestRepository.saveAll(requests);

        return updatedRequests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId, RequestStatusUpdateRequest request) {
        List<ParticipationRequestDto> updatedRequests = processRequestsStatus(
                userId, eventId, request.getRequestIds(), request.getStatus().name());

        List<ParticipationRequestDto> confirmed = updatedRequests.stream()
                .filter(r -> r.getStatus() == ParticipationRequest.ParticipationRequestStatus.CONFIRMED)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejected = updatedRequests.stream()
                .filter(r -> r.getStatus() == ParticipationRequest.ParticipationRequestStatus.REJECTED)
                .collect(Collectors.toList());

        return RequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        log.info("=== DEBUG toDto ===");
        log.info("Request ID: {}", request.getId());
        log.info("Requester ID: {}", request.getRequester().getId());
        log.info("Event ID: {}", request.getEvent().getId());
        log.info("Status: {}", request.getStatus());
        log.info("Created: {}", request.getCreated());

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }

    private ParticipationRequestDto updateRequestStatus(Long userId, Long eventId,
                                                        Long reqId, ParticipationRequest.ParticipationRequestStatus status) {
        // Проверка, что событие принадлежит пользователю
        // Используем метод с JOIN FETCH
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или доступ запрещен"));

        // Нужно получить запрос с JOIN FETCH
        // Сначала найдем его обычным способом, потом загрузим с зависимостями
        ParticipationRequest request = requestRepository.findById(reqId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + reqId + " не найден"));

        // Загружаем запрос с зависимостями для корректной работы toDto
        ParticipationRequest requestWithDependencies = requestRepository.findByIdAndRequesterId(reqId, request.getRequester().getId())
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + reqId + " не найден"));

        // Проверка, что запрос принадлежит событию
        if (!requestWithDependencies.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Запрос не принадлежит указанному событию");
        }

        // Проверка лимита участников для подтверждения
        if (status == ParticipationRequest.ParticipationRequestStatus.CONFIRMED) {
            Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        // Нельзя отклонить уже подтвержденный запрос
        if (status == ParticipationRequest.ParticipationRequestStatus.REJECTED &&
                requestWithDependencies.getStatus() == ParticipationRequest.ParticipationRequestStatus.CONFIRMED) {
            throw new ConflictException("Нельзя отклонить уже подтвержденный запрос");
        }

        requestWithDependencies.setStatus(status);
        ParticipationRequest updatedRequest = requestRepository.save(requestWithDependencies);

        return toDto(updatedRequest);
    }

}
