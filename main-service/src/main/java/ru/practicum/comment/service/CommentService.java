package ru.practicum.comment.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentState;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.NotPublishEventException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService implements CommentServiceInterface {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;


    @Override
    public CommentFullDto addComment(@Valid NewCommentDto dto, Long eventId, Long userId) {
        Optional<Event> event = eventRepository.findByIdAndInitiatorId(eventId, userId);

        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId + " or user: " + userId);
            throw new NotFoundException("there is no such event: " + eventId + " or user: " + userId);
        }

        if (!event.get().getState().equals(EventState.PUBLISHED)) {
            throw new NotPublishEventException(String.format("Event with id %s is not published", eventId));
        }

        Comment newComment = CommentMapper.toEntity(dto);
        User user = userRepository.getReferenceById(userId);
        newComment.setAuthor(user);
        newComment.setEvent(event.get());
        Comment createdComment = commentRepository.save(newComment);

        return CommentMapper.toFullDto(createdComment);

    }

    @Override
    public CommentFullDto updateCommentStatusByAdmin(Long eventId, Long commentId, boolean published) {
        Optional<Comment> commentOpt = commentRepository.findByIdAndEventId(commentId, eventId);

        if (commentOpt.isEmpty()) {
            log.error("Comment with id {} not found for event {}", commentId, eventId);
            throw new NotFoundException("Comment not found for the specified event");
        }

        Comment comment = commentOpt.get();
        comment.setState(published ? CommentState.PUBLIC : CommentState.HIDDEN);

        Comment updatedComment = commentRepository.save(comment);

        log.info("Comment with id {} status changed to {}", commentId,
                published ? "PUBLIC" : "HIDDEN");

        return CommentMapper.toFullDto(updatedComment);
    }

    @Override
    public List<CommentFullDto> getPublicCommentsByEvent(Long eventId) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);

        if (eventOpt.isEmpty()) {
            log.error("Event with id {} not found", eventId);
            throw new NotFoundException("Event with id " + eventId + " not found");
        }

        Event event = eventOpt.get();

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Event with id {} is not published", eventId);
            throw new NotPublishEventException("Event with id " + eventId + " is not published");
        }

        List<Comment> comments = commentRepository.findPublicCommentsByEventId(eventId, CommentState.PUBLIC);
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream()
                .map(CommentMapper::toFullDto)
                .toList();
    }


    @Override
    public CommentFullDto updateComment(Long userId, Long eventId, Long commentId, @Valid UpdCommentDto updDto) {
        Optional<Event> event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId + " or user: " + userId);
            throw new NotFoundException("there is no such event: " + eventId + " or user: " + userId);
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            log.error("there is no such comment: " + commentId);
            throw new NotFoundException("there is no such comment: " + commentId);
        }
        Comment commentToUpdate = comment.get();
        commentToUpdate.setText(updDto.getText());
        commentRepository.save(commentToUpdate);
        return CommentMapper.toFullDto(commentToUpdate);
    }


    @Override
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        Optional<Event> event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId + " or user: " + userId);
            throw new NotFoundException("there is no such event: " + eventId + " or user: " + userId);
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            log.error("there is no such comment: " + commentId);
            throw new NotFoundException("there is no such comment: " + commentId);
        }
        commentRepository.delete(comment.get());
    }


    @Override
    public List<CommentFullDto> getAllCommentsBy(Long userId, Long eventId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.error("there is no such user: " + userId);
            throw new NotFoundException("there is no such user: " + userId);
        }
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId);
            throw new NotFoundException("there is no such event: " + eventId);
        }
        List<Comment> commentList = commentRepository.findByAuthorIdAndEventId(user.get().getId(), event.get().getId());
        if (commentList.isEmpty()) {
            return Collections.emptyList();
        }
        return commentList.stream()
                .map(CommentMapper::toFullDto)
                .toList();
    }

    public CommentFullDto getCommentByCommentId(Long userId, Long eventId, Long commentId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.error("there is no such user: " + userId);
            throw new NotFoundException("there is no such user: " + userId);
        }
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId);
            throw new NotFoundException("there is no such event: " + eventId);
        }
        Optional<Comment> comment = commentRepository.findByIdAndAuthorIdAndEventId(commentId, userId, eventId);
        if (comment.isEmpty()) {
            log.error("there is no such comment with id: " + commentId);
            throw new NotFoundException("there is no such comment with id: " + commentId);
        }
        return CommentMapper.toFullDto(comment.get());
    }

    public CommentFullDto getPublicCommentById(Long eventId, Long commentId) {
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId);
            throw new NotFoundException("there is no such event: " + eventId);
        }
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            log.error("there is no such comment with id: " + commentId);
            throw new NotFoundException("there is no such comment with id: " + commentId);
        }
        return CommentMapper.toFullDto(comment.get());
    }
}
