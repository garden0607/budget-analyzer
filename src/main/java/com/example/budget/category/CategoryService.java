package com.example.budget.category;   // category 패키지

import com.example.budget.budget.BudgetRepository;              // ★ 추가: 예산 저장소 (삭제 시 예산 먼저 지우기)
import com.example.budget.category.dto.CategoryCreateRequest;   // 추가 요청 상자
import com.example.budget.category.dto.CategoryResponse;        // 응답 상자
import com.example.budget.category.dto.CategoryUpdateRequest;   // ★ 추가: 수정 요청 상자
import com.example.budget.global.exception.BusinessException;   // 우리가 만든 예외
import com.example.budget.global.exception.ErrorCode;           // 오류 목록표
import com.example.budget.transaction.TransactionRepository;    // ★ 추가: 거래 저장소 (사용 중인지 확인)
import com.example.budget.user.User;                            // 회원 엔티티
import com.example.budget.user.UserRepository;                  // 회원 저장소
import lombok.RequiredArgsConstructor;                          // final 필드 생성자 자동 생성
import org.springframework.stereotype.Service;                  // Service 표시
import org.springframework.transaction.annotation.Transactional; // 트랜잭션

import java.util.List;   // 여러 개 결과

@Service                        // 스프링이 Service로 등록·관리
@RequiredArgsConstructor        // final 필드를 받는 생성자 자동 생성 (의존성 주입)
@Transactional(readOnly = true) // 기본은 읽기 전용
public class CategoryService {

    private final CategoryRepository categoryRepository;         // 카테고리 DB 작업
    private final UserRepository userRepository;                 // 회원 DB 작업
    private final TransactionRepository transactionRepository;   // ★ 추가: 거래 DB 작업
    private final BudgetRepository budgetRepository;             // ★ 추가: 예산 DB 작업

    // ───────────── 조회 (FR-03) ─────────────
    public List<CategoryResponse> getCategories(Long userId, TransactionType type) {
        List<Category> categories = (type == null)
                ? categoryRepository.findByUserId(userId)                 // 구분 없으면 전부
                : categoryRepository.findByUserIdAndType(userId, type);   // 있으면 그 구분만
        return categories.stream()
                .map(CategoryResponse::from)   // 엔티티 → 응답 상자
                .toList();
    }

    // ───────────── 추가 (FR-04) ─────────────
    @Transactional
    public CategoryResponse create(Long userId, CategoryCreateRequest request) {
        String name = request.name().strip();   // 앞뒤 공백 제거

        if (categoryRepository.existsByUserIdAndTypeAndName(userId, request.type(), name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY);   // 409 이름 중복
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));   // 404 회원 없음

        Category category = Category.builder()
                .user(user)
                .name(name)
                .type(request.type())
                .isDefault(false)   // 직접 추가 → 기본 카테고리 아님
                .build();
        categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    // ───────────── 수정 (FR-04) ─────────────  ★ 추가
    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request) {
        Category category = findMyCategory(userId, categoryId);   // 1. 본인 카테고리인지 확인 (아니면 404)
        String name = request.name().strip();                     // 앞뒤 공백 제거

        if (category.getName().equals(name)) {                    // 2. 지금 이름과 같으면
            return CategoryResponse.from(category);               //    바꿀 게 없으니 그대로 돌려줌
        }

        if (categoryRepository.existsByUserIdAndTypeAndName(userId, category.getType(), name)) {  // 3. 같은 구분에 같은 이름 있는지
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY);                            //    있으면 409
        }

        category.changeName(name);                // 4. 이름 변경 (save 안 해도 트랜잭션 끝날 때 자동 UPDATE)
        return CategoryResponse.from(category);   // 바뀐 정보 돌려줌
    }

    // ───────────── 삭제 (FR-04) ─────────────  ★ 추가
    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = findMyCategory(userId, categoryId);   // 1. 본인 카테고리인지 확인 (아니면 404)

        if (transactionRepository.existsByCategoryId(categoryId)) {     // 2. 이 카테고리를 쓰는 거래가 있는지
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);     //    있으면 409 "사용 중인 카테고리는 삭제할 수 없습니다."
        }

        budgetRepository.deleteByCategoryId(categoryId);   // 3. 이 카테고리의 예산 먼저 삭제 (FK 때문에 순서 중요)
        categoryRepository.delete(category);               // 4. 카테고리 삭제
    }

    // ───────────── 본인 카테고리 찾기 (수정·삭제 공통) ─────────────  ★ 추가
    private Category findMyCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)                       // id와 주인이 둘 다 맞아야 찾아짐
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));      // 없거나 남의 것이면 404
    }
}