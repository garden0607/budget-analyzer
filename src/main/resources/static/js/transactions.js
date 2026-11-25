// ===== 거래 내역 화면(S-04) 동작 =====
// apiFetch는 api.js의 함수. 헤더가 있는 화면이라 토큰이 없으면 api.js가 로그인 화면으로 보냄

// ----- 화면 요소 찾기 -----
const listBody = document.getElementById('transaction-list');     // 거래 표 몸통
const errorMsg = document.getElementById('error-msg');            // 에러 문구 자리
const searchStart = document.getElementById('search-start');      // 검색: 시작일
const searchEnd = document.getElementById('search-end');          // 검색: 종료일
const searchCategory = document.getElementById('search-category');// 검색: 카테고리
const searchType = document.getElementById('search-type');        // 검색: 구분
const incomeTotalEl = document.getElementById('income-total');    // 합계 줄: 수입
const expenseTotalEl = document.getElementById('expense-total');  // 합계 줄: 지출
const netTotalEl = document.getElementById('net-total');          // 합계 줄: 수입 − 지출
const totalCountEl = document.getElementById('total-count');      // "(총 45건)"
const pageInfoEl = document.getElementById('page-info');          // "1 / 3 페이지"
const prevBtn = document.getElementById('prev-page');             // [이전]
const nextBtn = document.getElementById('next-page');             // [다음]

// ----- 화면 상태 (바뀌는 값이라 let) -----
let currentPage = 0;            // 지금 보고 있는 페이지 (서버 규칙대로 0부터)
let categories = [];            // 내 카테고리 목록 (검색 상자, 나중에 모달에서도 씀)

// ----- 금액 표시: 9000 → "9,000원" -----
function formatMoney(amount) {
    return amount.toLocaleString('ko-KR') + '원';  // 천 단위 쉼표 + "원"
}

// ----- 검색 조건 → 주소 뒤 "?..." 만들기 (값이 있는 조건만) -----
function buildQuery() {
    const params = new URLSearchParams();          // 빈 조건 묶음
    if (searchStart.value) params.set('startDate', searchStart.value);      // 시작일이 있으면 추가
    if (searchEnd.value) params.set('endDate', searchEnd.value);            // 종료일이 있으면 추가
    if (searchCategory.value) params.set('categoryId', searchCategory.value); // "전체"(빈 값)가 아니면 추가
    if (searchType.value) params.set('type', searchType.value);             // "전체"가 아니면 추가
    params.set('page', currentPage);               // 페이지 번호는 항상
    return params.toString();                      // "startDate=2026-11-01&page=0" 같은 글자
}

// ----- 카테고리 선택 상자 채우기 (지출 묶음 / 수입 묶음) -----
async function loadCategoryOptions() {
    categories = await apiFetch('/api/categories');                // 내 카테고리 전체
    const groups = [['EXPENSE', '지출'], ['INCOME', '수입']];        // [type 값, 묶음 제목]
    groups.forEach(([type, label]) => {                            // 지출, 수입 순서로
        const group = document.createElement('optgroup');          // 묶음 만들기
        group.label = label;                                       // 묶음 제목 ("지출")
        categories.filter(c => c.type === type).forEach(c => {     // 그 구분의 카테고리만
            const option = document.createElement('option');       // 선택지 하나
            option.value = c.id;                                   // 서버로 보낼 값: 카테고리 id
            option.textContent = c.name;                           // 화면 글자: 이름 (textContent → XSS 방지)
            group.appendChild(option);
        });
        searchCategory.appendChild(group);                         // 선택 상자에 묶음 붙이기
    });
}

// ----- 거래 목록 불러오기: API 호출 → 합계·표·페이지 그리기 -----
async function loadTransactions() {
    errorMsg.textContent = '';                                     // 이전 에러 지우기
    try {
        const data = await apiFetch('/api/transactions?' + buildQuery()); // 조건 붙여서 조회
        searchStart.value = data.startDate;                        // 서버가 실제로 적용한 기간으로 날짜 칸 채우기
        searchEnd.value = data.endDate;                            // (처음엔 이번 달 1일 ~ 말일)
        renderSummary(data);                                       // 합계 줄
        renderRows(data.transactions);                             // 표
        renderPagination(data);                                    // 페이지 정보·버튼
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

// ----- 합계 줄 그리기 -----
function renderSummary(data) {
    const net = data.incomeTotal - data.expenseTotal;              // 수입 − 지출
    incomeTotalEl.textContent = formatMoney(data.incomeTotal);
    expenseTotalEl.textContent = formatMoney(data.expenseTotal);
    netTotalEl.textContent = (net >= 0 ? '+' : '-') + formatMoney(Math.abs(net)); // 부호 붙이기 (+1,670,000원)
    netTotalEl.className = net >= 0 ? 'amount-income' : 'amount-expense';       // 남으면 파랑, 모자라면 빨강
    totalCountEl.textContent = `(총 ${data.totalCount}건)`;
}

// ----- 표 그리기 -----
function renderRows(transactions) {
    listBody.replaceChildren();                                    // 표 비우기

    if (transactions.length === 0) {                               // 거래가 없으면 안내 한 줄
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 5;                                            // 칸 5개를 합쳐 한 칸으로
        td.className = 'empty';
        td.textContent = '거래 내역이 없습니다.';
        tr.appendChild(td);
        listBody.appendChild(tr);
        return;
    }

    transactions.forEach(t => listBody.appendChild(createRow(t))); // 거래마다 한 줄씩
}

// ----- 거래 한 줄 만들기: [날짜] [카테고리] [메모] [금액] [수정][삭제] -----
function createRow(t) {
    const tr = document.createElement('tr');

    const dateTd = document.createElement('td');
    dateTd.textContent = t.transactionDate;                        // "2026-11-05"

    const categoryTd = document.createElement('td');
    categoryTd.textContent = t.categoryName;                       // "식비"

    const memoTd = document.createElement('td');
    memoTd.textContent = t.memo ?? '';                             // 메모 (없으면 빈칸). 사용자 입력 → textContent

    const amountTd = document.createElement('td');
    const isIncome = t.type === 'INCOME';                          // 수입인지
    amountTd.textContent = (isIncome ? '+' : '-') + formatMoney(t.amount); // "+2,500,000원" / "-9,000원"
    amountTd.className = 'text-right ' + (isIncome ? 'amount-income' : 'amount-expense'); // 오른쪽 정렬 + 색

    const actionTd = document.createElement('td');
    actionTd.className = 'col-actions';
    const editBtn = document.createElement('button');
    editBtn.type = 'button';
    editBtn.className = 'btn btn-sm btn-outline';
    editBtn.textContent = '수정';                                   // 동작은 수정 단계에서 연결
    const deleteBtn = document.createElement('button');
    deleteBtn.type = 'button';
    deleteBtn.className = 'btn btn-sm btn-danger';
    deleteBtn.textContent = '삭제';                                 // 동작은 삭제 단계에서 연결
    editBtn.addEventListener('click', () => openModal(t));          // 수정 → 이 거래 값으로 모달 열기
    deleteBtn.addEventListener('click', () => deleteTransaction(t)); // 삭제 → 확인 후 삭제
    actionTd.append(editBtn, deleteBtn);

    tr.append(dateTd, categoryTd, memoTd, amountTd, actionTd);
    return tr;
}

// ----- 페이지 정보·버튼 -----
function renderPagination(data) {
    const totalPages = Math.max(data.totalPages, 1);               // 거래가 0건이어도 "1 / 1"로 보이게
    pageInfoEl.textContent = `${data.page + 1} / ${totalPages} 페이지`; // 서버는 0부터, 화면은 1부터
    prevBtn.disabled = data.page <= 0;                             // 첫 페이지면 [이전] 잠금
    nextBtn.disabled = data.page + 1 >= totalPages;                // 마지막 페이지면 [다음] 잠금
}

// ----- 화면이 열리면: 카테고리 먼저 채우고 → 목록 불러오기 -----
async function init() {
    try {
        await loadCategoryOptions();                               // 선택 상자 먼저
    } catch (e) {
        errorMsg.textContent = e.message;
    }
    await loadTransactions();                                      // 그다음 이번 달 거래
}

init();
// ----- 검색: [조회] 클릭 또는 Enter → 1페이지부터 다시 조회 -----
document.getElementById('search-form').addEventListener('submit', (event) => {
    event.preventDefault();                        // 페이지 새로고침 막기
    currentPage = 0;                               // 조건이 바뀌었으니 첫 페이지부터
    loadTransactions();                            // buildQuery()가 지금 입력된 조건을 읽어 조회
});

// ----- 페이지 이동: [이전] / [다음] -----
prevBtn.addEventListener('click', () => {
    currentPage--;                                 // 페이지 번호 1 줄이기 (잠겨 있으면 클릭 자체가 안 됨)
    loadTransactions();                            // 같은 조건, 바뀐 페이지로 다시 조회
});

nextBtn.addEventListener('click', () => {
    currentPage++;                                 // 페이지 번호 1 늘리기
    loadTransactions();
});
// ===================== 거래 등록·수정 모달 =====================
const modal = document.getElementById('tx-modal');            // 모달 전체 (반투명 막)
const modalTitle = document.getElementById('modal-title');    // 모달 제목
const txForm = document.getElementById('tx-form');            // 모달 form
const txCategory = document.getElementById('tx-category');    // 카테고리 선택 상자
const txAmount = document.getElementById('tx-amount');        // 금액
const txDate = document.getElementById('tx-date');            // 날짜
const txMemo = document.getElementById('tx-memo');            // 메모
const modalError = document.getElementById('modal-error');    // 모달 안 에러 문구
const saveBtn = txForm.querySelector('button[type="submit"]');// 저장 버튼

let editingId = null;          // null이면 등록, 숫자면 그 id의 거래를 수정 중

// ----- 오늘 날짜를 "2026-11-25" 모양으로 (내 컴퓨터 시간 기준) -----
function today() {
    const d = new Date();                                      // 지금 시각
    const mm = String(d.getMonth() + 1).padStart(2, '0');      // 월 (0부터 세서 +1), 한 자리면 앞에 0
    const dd = String(d.getDate()).padStart(2, '0');           // 일, 한 자리면 앞에 0
    return `${d.getFullYear()}-${mm}-${dd}`;
}

// ----- 모달 카테고리 선택 상자 채우기 (처음 열 때 한 번만) -----
function fillModalCategories() {
    if (txCategory.options.length > 0) return;                 // 이미 채웠으면 건너뜀
    const placeholder = document.createElement('option');      // 맨 위 안내 선택지
    placeholder.value = '';                                    // 빈 값 → 안 고르면 서버가 "카테고리를 선택해 주세요."
    placeholder.textContent = '카테고리 선택';
    txCategory.appendChild(placeholder);

    [['EXPENSE', '지출'], ['INCOME', '수입']].forEach(([type, label]) => { // 지출, 수입 묶음
        const group = document.createElement('optgroup');
        group.label = label;
        categories.filter(c => c.type === type).forEach(c => { // 4단계에서 받아 둔 카테고리 재사용
            const option = document.createElement('option');
            option.value = c.id;                               // 보낼 값: 카테고리 id
            option.textContent = c.name;                       // 보일 글자: 이름
            group.appendChild(option);
        });
        txCategory.appendChild(group);
    });
}

// ----- 모달 열기: tx가 없으면 등록, 있으면 수정 -----
function openModal(tx) {
    fillModalCategories();                                     // 선택 상자 준비
    modalError.textContent = '';                               // 이전 에러 지우기

    if (tx) {                                                  // 수정: 기존 값 채우기
        editingId = tx.id;
        modalTitle.textContent = '거래 수정';
        txCategory.value = tx.categoryId;                      // 원래 카테고리가 선택된 상태로
        txAmount.value = tx.amount;
        txDate.value = tx.transactionDate;
        txMemo.value = tx.memo ?? '';
    } else {                                                   // 등록: 빈 칸 + 날짜는 오늘
        editingId = null;
        modalTitle.textContent = '거래 등록';
        txForm.reset();                                        // form 안 입력칸 전부 비우기
        txDate.value = today();
    }

    modal.hidden = false;                                      // 모달 보이기
    txAmount.focus();                                          // 금액 칸에 바로 커서 (바로 입력 가능)
}

// ----- 모달 닫기 -----
function closeModal() {
    modal.hidden = true;                                       // 숨기기
    editingId = null;                                          // 수정 상태 초기화
}

// ----- 저장: 등록이면 POST, 수정이면 PUT -----
txForm.addEventListener('submit', async (event) => {
    event.preventDefault();                                    // 새로고침 막기
    modalError.textContent = '';

    const body = {                                             // TransactionRequest와 같은 이름
        categoryId: txCategory.value ? Number(txCategory.value) : null, // 선택 상자 값은 글자라 숫자로. 안 골랐으면 null
        amount: txAmount.value === '' ? null : Number(txAmount.value),  // 비었으면 null → 서버가 "금액을 입력해 주세요."
        transactionDate: txDate.value || null,                 // "2026-11-25", 비었으면 null
        memo: txMemo.value.trim() || null,                     // 메모 없으면 null
    };

    const isEdit = editingId !== null;                         // 수정 중인지
    const before = await getUnreadNotifications();             // ★ 저장 전 안 읽은 알림 (비교용)
    saveBtn.disabled = true;                                   // 중복 저장 방지
    try {
        await apiFetch(isEdit ? `/api/transactions/${editingId}` : '/api/transactions', {
            method: isEdit ? 'PUT' : 'POST',                   // 수정이면 PUT, 등록이면 POST
            body: body,
        });
        closeModal();                                          // 성공 → 닫기
        await loadTransactions();                              // 목록·합계 다시 그리기
        await showNewAlerts(before);                           // ★ 새 예산 알림이 생겼으면 띠로 표시
    } catch (e) {
        modalError.textContent = e.message;                    // 실패 → 모달 안에 서버 문구 (모달은 열어 둠)
    } finally {
        saveBtn.disabled = false;
    }
});

// ----- 여는 버튼과 닫는 방법 3가지 -----
document.getElementById('open-create').addEventListener('click', () => openModal()); // [거래 등록] → 빈 모달
document.getElementById('modal-cancel').addEventListener('click', closeModal);       // [취소]
modal.addEventListener('click', (event) => {                   // 모달 바깥(검은 막)을 누르면
    if (event.target === modal) closeModal();                  // 흰 창 안을 누른 게 아닐 때만 닫기
});
document.addEventListener('keydown', (event) => {              // 키보드
    if (event.key === 'Escape' && !modal.hidden) closeModal(); // Esc 누르면 닫기
});
// ----- 거래 삭제: 확인 → DELETE → 목록 다시 그리기 -----
async function deleteTransaction(t) {
    const label = `${t.transactionDate} ${t.categoryName} ${formatMoney(t.amount)}`; // 확인 창에 보여 줄 내용
    if (!confirm(`이 거래를 삭제할까요?\n${label}`)) return;  // 취소면 아무것도 안 함
    errorMsg.textContent = '';

    try {
        await apiFetch(`/api/transactions/${t.id}`, { method: 'DELETE' }); // 성공하면 204 → null
        await loadTransactions();                              // 목록·합계 다시 그리기
    } catch (e) {
        errorMsg.textContent = e.message;                      // 실패 문구
        errorMsg.scrollIntoView({ behavior: 'smooth', block: 'center' }); // 문구가 보이게 화면 이동
    }
}
// ===================== 거래 후 예산 알림 표시 (FR-05, 방법 A) =====================

// ----- 안 읽은 알림 조회 (실패하면 null: 알림 때문에 화면이 멈추지 않게) -----
async function getUnreadNotifications() {
    try {
        return await apiFetch('/api/notifications?unreadOnly=true'); // { unreadCount, notifications(최신순) }
    } catch (e) {
        return null;                                   // 거래 저장은 이미 끝났으니 조용히 넘어감
    }
}

// ----- 저장 전(before)과 비교해서 새로 생긴 알림만 띄우기 -----
async function showNewAlerts(before) {
    const after = await getUnreadNotifications();      // 저장 후 다시 조회
    loadUnreadCount();                                 // api.js 함수: 헤더 🔔 숫자 바로 갱신
    if (!before || !after) return;                     // 둘 중 하나라도 조회 실패면 비교 불가 → 끝

    const added = after.unreadCount - before.unreadCount; // 늘어난 개수
    if (added <= 0) return;                            // 안 늘었으면 새 알림 없음

    after.notifications                                // 최신순 목록에서
        .slice(0, added)                               // 앞에서부터 늘어난 개수만큼 = 이번에 생긴 알림
        .forEach(n => showToast(n.message, n.type));   // 하나씩 띠로 표시
}

// ----- 토스트 띄우기: 4초 뒤 자동으로 사라짐 -----
function showToast(message, type) {
    let area = document.querySelector('.toast-area');  // 토스트 모아 둘 자리 찾기
    if (!area) {                                       // 없으면 처음 한 번 만들기
        area = document.createElement('div');
        area.className = 'toast-area';
        document.body.appendChild(area);
    }

    const toast = document.createElement('div');
    toast.className = 'toast ' + (type === 'EXCEEDED' ? 'toast-danger' : 'toast-warning'); // 초과 빨강, 경고 주황
    toast.textContent = '⚠️ ' + message;               // 서버가 만든 문구 (textContent로 안전하게)
    area.appendChild(toast);

    setTimeout(() => toast.remove(), 4000);            // 4000ms(4초) 뒤에 이 띠 삭제
}