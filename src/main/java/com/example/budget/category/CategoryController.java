package com.example.budget.category;   // category 패키지

import com.example.budget.category.dto.CategoryCreateRequest;   // 추가 요청 상자
import com.example.budget.category.dto.CategoryResponse;        // 응답 상자
import com.example.budget.category.dto.CategoryUpdateRequest;   // 수정 요청 상자
import jakarta.validation.Valid;                                // DTO 검증 실행
import lombok.RequiredArgsConstructor;                          // final 필드 생성자 자동 생성
import org.springframework.http.HttpStatus;                     // 상태 코드 목록
import org.springframework.http.ResponseEntity;                 // 상태 코드 + 내용 응답
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 토큰의 회원 id 꺼내기
import org.springframework.web.bind.annotation.*;               // @GetMapping, @PostMapping 등 전체

import java.util.List;   // 여러 개 결과

@RestController                    // API 창구, 돌려주는 값은 자동으로 JSON
@RequestMapping("/api/categories") // 이 클래스의 모든 주소 앞에 /api/categories
@RequiredArgsConstructor           // final 필드 생성자 자동 생성
public class CategoryController {

    private final CategoryService categoryService;   // 실제 처리는 Service에 맡김

    // 조회: GET /api/categories  또는  GET /api/categories?type=EXPENSE
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal Long userId,                          // 필터가 기록한 회원 id (토큰에서 꺼낸 값)
            @RequestParam(required = false) TransactionType type) {        // ?type=... (안 보내도 됨 → null)
        return ResponseEntity.ok(categoryService.getCategories(userId, type));   // 200 + 카테고리 목록
    }

    // 추가: POST /api/categories
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal Long userId,                          // 회원 id
            @Valid @RequestBody CategoryCreateRequest request) {           // JSON → 상자 + 검증
        CategoryResponse response = categoryService.create(userId, request);   // 추가하고 결과 받기
        return ResponseEntity.status(HttpStatus.CREATED).body(response);       // 201 + 만들어진 카테고리
    }

    // 수정: PUT /api/categories/15
    @PutMapping("/{categoryId}")                                           // {categoryId} 자리에 숫자가 들어옴
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal Long userId,                          // 회원 id
            @PathVariable Long categoryId,                                 // 주소의 15를 꺼냄
            @Valid @RequestBody CategoryUpdateRequest request) {           // 새 이름 + 검증
        return ResponseEntity.ok(categoryService.update(userId, categoryId, request));   // 200 + 바뀐 카테고리
    }

    // 삭제: DELETE /api/categories/15
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,                          // 회원 id
            @PathVariable Long categoryId) {                               // 주소의 15를 꺼냄
        categoryService.delete(userId, categoryId);                        // 삭제 처리
        return ResponseEntity.noContent().build();                         // 204 No Content (내용 없이 성공)
    }
}