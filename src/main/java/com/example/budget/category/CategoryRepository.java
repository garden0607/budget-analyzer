package com.example.budget.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndType(Long userId, TransactionType type);

    List<Category> findByUserId(Long userId);

    boolean existsByUserIdAndTypeAndName(Long userId, TransactionType type, String name);

    Optional<Category> findByIdAndUserId(Long id, Long userId);
}