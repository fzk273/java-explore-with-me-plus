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
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.exception.NotFoundException;
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
        Comment newComment = CommentMapper.toEntity(dto);
        User user = userRepository.getReferenceById(userId);
        newComment.setAuthor(user);
        newComment.setEvent(event.get());
        Comment createdComment = commentRepository.save(newComment);
        return CommentMapper.toFullDto(createdComment);

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
            log.error("there is no such user: " + user);
            throw new NotFoundException("there is no such user: " + user);
        }
        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            log.error("there is no such event: " + eventId);
            throw new NotFoundException("there is no such event: " + eventId);
        }
        List<Comment> commentList = commentRepository.findByUserIdAndEventId(user.get().getId(), event.get().getId());
        if (commentList.isEmpty()) {
            return Collections.emptyList();
        }
        return commentList.stream()
                .map(CommentMapper::toFullDto)
                .toList();
    }
}
