// ===== 카테고리 화면(S-08) 동작 =====
// apiFetch는 api.js의 함수. 헤더가 있는 화면이라 토큰이 없으면 api.js가 로그인 화면으로 보냄

const expenseList = document.getElementById('expense-list'); // 지출 표 몸통 (줄을 넣을 곳)
const incomeList = document.getElementById('income-list');   // 수입 표 몸통
const errorMsg = document.getElementById('error-msg');       // 에러 문구 자리

// ----- 목록 불러오기: API 호출 → 지출/수입으로 나눠 그리기 -----
async function loadCategories() {
    try {
        const categories = await apiFetch('/api/categories');           // 내 카테고리 전체 (배열)
        renderList(expenseList, categories.filter(c => c.type === 'EXPENSE')); // 지출만 골라 지출 표에
        renderList(incomeList, categories.filter(c => c.type === 'INCOME'));   // 수입만 골라 수입 표에
    } catch (e) {
        errorMsg.textContent = e.message;                                // 실패하면 문구 표시
    }
}

// ----- 표 하나 그리기: 기존 줄을 지우고 카테고리마다 줄 추가 -----
function renderList(tbody, categories) {
    tbody.replaceChildren();                               // 표 몸통 비우기 (다시 그릴 때 중복 방지)

    if (categories.length === 0) {                         // 카테고리가 하나도 없으면
        const tr = document.createElement('tr');           // 줄 하나 만들고
        const td = document.createElement('td');           // 칸 하나 만들어서
        td.colSpan = 2;                                    // 칸 2개(이름·관리)를 합쳐 한 칸으로
        td.className = 'empty';                            // 흐린 가운데 글씨 (CSS .empty)
        td.textContent = '카테고리가 없습니다.';             // 안내 문구
        tr.appendChild(td);                                // 줄에 칸 넣기
        tbody.appendChild(tr);                             // 표에 줄 넣기
        return;                                            // 여기서 끝
    }

    categories.forEach(category => {                       // 카테고리 하나씩 꺼내서
        tbody.appendChild(createRow(category));            // 줄을 만들어 표에 붙이기
    });
}

// ----- 카테고리 한 줄 만들기: [이름 (기본)] [수정] [삭제] -----
function createRow(category) {
    const tr = document.createElement('tr');               // 줄

    const nameTd = document.createElement('td');           // 이름 칸
    nameTd.textContent = category.name;                    // 이름은 사용자 입력값 → 꼭 textContent (XSS 방지)
    if (category.isDefault) {                              // 기본 카테고리면
        const tag = document.createElement('span');        // 작은 꼬리표 만들기
        tag.className = 'tag';                             // CSS .tag (옅은 회색 상자)
        tag.textContent = '기본';
        nameTd.appendChild(tag);                           // 이름 옆에 붙이기
    }

    const actionTd = document.createElement('td');         // 관리 칸
    actionTd.className = 'col-actions';                    // 폭 고정, 오른쪽 정렬 (CSS)

    const editBtn = document.createElement('button');      // 수정 버튼
    editBtn.type = 'button';                               // form 제출 버튼이 아니라 일반 버튼
    editBtn.className = 'btn btn-sm btn-outline';          // 작은 흰 버튼
    editBtn.textContent = '수정';                           // 동작은 6단계에서 연결

    const deleteBtn = document.createElement('button');    // 삭제 버튼
    deleteBtn.type = 'button';
    deleteBtn.className = 'btn btn-sm btn-danger';         // 작은 빨간 버튼
    deleteBtn.textContent = '삭제';                         // 동작은 6단계에서 연결
    editBtn.addEventListener('click', () => editCategory(category));     // 수정 버튼 클릭 → 이 줄의 카테고리로 수정
    deleteBtn.addEventListener('click', () => deleteCategory(category)); // 삭제 버튼 클릭 → 이 줄의 카테고리로 삭제

    actionTd.append(editBtn, deleteBtn);                   // 관리 칸에 버튼 두 개 넣기
    tr.append(nameTd, actionTd);                           // 줄에 이름 칸·관리 칸 넣기
    return tr;                                             // 완성된 줄을 돌려줌
}

loadCategories();                                          // 화면이 열리면 바로 목록 불러오기

// ----- 카테고리 추가: form 제출 → POST → 목록 다시 그리기 -----
const addForm = document.getElementById('add-form');       // 추가 form
const addType = document.getElementById('add-type');       // 구분 선택 상자
const addName = document.getElementById('add-name');       // 이름 입력칸
const addBtn = addForm.querySelector('button[type="submit"]'); // 추가 버튼

addForm.addEventListener('submit', async (event) => {      // form 제출 시 (버튼 클릭 또는 Enter)
    event.preventDefault();                                // 페이지 새로고침 막기
    errorMsg.textContent = '';                             // 이전 에러 문구 지우기

    addBtn.disabled = true;                                // 응답 올 때까지 버튼 잠금 (중복 추가 방지)
    try {
        await apiFetch('/api/categories', {                // 추가 API 호출
            method: 'POST',                                // 새로 만드는 거라 POST
            body: {
                name: addName.value.trim(),                // 이름 (앞뒤 공백 제거)
                type: addType.value,                       // 선택한 option의 value: "EXPENSE" 또는 "INCOME"
            },
        });
        addName.value = '';                                // 성공하면 입력칸 비우기
        await loadCategories();                            // 목록 다시 불러와 그리기 → 새 카테고리 표시
    } catch (e) {
        errorMsg.textContent = e.message;                  // 실패 문구 (빈 이름, 중복 이름 등 서버 문구 그대로)
    } finally {
        addBtn.disabled = false;                           // 버튼 다시 풀기
    }
});
// ----- 카테고리 이름 수정: 새 이름 입력 → PUT → 목록 다시 그리기 -----
async function editCategory(category) {
    const newName = prompt(`'${category.name}'의 새 이름을 입력해 주세요.`, category.name); // 입력 창 (기본값: 지금 이름)
    if (newName === null) return;                          // 취소를 누르면 아무것도 안 함
    errorMsg.textContent = '';                             // 이전 에러 문구 지우기

    try {
        await apiFetch(`/api/categories/${category.id}`, { // 주소에 이 카테고리의 id를 넣음 (예: /api/categories/15)
            method: 'PUT',                                 // 수정은 PUT
            body: { name: newName.trim() },                // 이름만 보냄 (구분은 수정 불가)
        });
        await loadCategories();                            // 목록 다시 그리기 → 바뀐 이름 표시
    } catch (e) {
        errorMsg.textContent = e.message;                  // 실패 문구 (빈 이름, 중복 이름 등)
    }
}

// ----- 카테고리 삭제: 확인 → DELETE → 목록 다시 그리기 -----
async function deleteCategory(category) {
    const ok = confirm(`'${category.name}' 카테고리를 삭제할까요?\n이 카테고리의 예산도 함께 삭제됩니다.`); // 확인 창
    if (!ok) return;                                       // 취소를 누르면 아무것도 안 함
    errorMsg.textContent = '';

    try {
        await apiFetch(`/api/categories/${category.id}`, { // 이 카테고리의 id로
            method: 'DELETE',                              // 삭제 요청 (성공하면 204 → apiFetch가 null 돌려줌)
        });
        await loadCategories();                            // 목록 다시 그리기 → 지운 줄 사라짐
    } catch (e) {
        errorMsg.textContent = e.message;                  // 거래가 있는 카테고리면 서버가 거부한 문구 표시
    }
}