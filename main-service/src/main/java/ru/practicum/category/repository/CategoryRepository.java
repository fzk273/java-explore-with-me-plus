package ru.practicum.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.category.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(value = "SELECT COUNT(*) > 0 FROM events WHERE category_id = :categoryId", nativeQuery = true)
    boolean existsEventsByCategoryId(@Param("categoryId") Long categoryId);
}