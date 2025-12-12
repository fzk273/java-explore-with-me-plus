package ru.practicum.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentState;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByAuthorIdAndEventId(Long userId, Long eventId);

    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.event.id = :eventId")
    Optional<Comment> findByIdAndEventId(@Param("commentId") Long commentId,
                                         @Param("eventId") Long eventId);

    // ИСПРАВЛЕНО: добавил сортировку и правильное название
    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.state = :state ORDER BY c.publishedOn DESC")
    List<Comment> findPublicCommentsByEventId(@Param("eventId") Long eventId,
                                              @Param("state") CommentState state);

    Long countByEventId(Long eventId);
}