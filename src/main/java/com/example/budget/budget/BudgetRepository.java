package com.example.budget.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByCategoryIdAndBudgetMonth(Long categoryId, String budgetMonth);

    List<Budget> findByUserIdAndBudgetMonth(Long userId, String budgetMonth);

    void deleteByCategoryId(Long categoryId);
}