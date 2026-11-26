// ===== 예산 화면(S-06) 동작 =====
// apiFetch는 api.js의 함수. 헤더가 있는 화면이라 토큰이 없으면 api.js가 로그인 화면으로 보냄

// ----- 화면 요소 찾기 -----
const listBody = document.getElementById('budget-list');          // 표 몸통
const errorMsg = document.getElementById('error-msg');            // 에러 문구 자리
const monthLabel = document.getElementById('month-label');        // "2026년 11월"
const totalBudgetEl = document.getElementById('total-budget');    // 합계: 예산
const totalSpentEl = document.getElementById('total-spent');      // 합계: 사용
const totalRemainingEl = document.getElementById('total-remaining'); // 합계: 잔여
const totalBar = document.getElementById('total-bar');            // 전체 사용률 막대 (안쪽)
const totalRate = document.getElementById('total-rate');          // 전체 사용률 숫자

// ----- 화면 상태 -----
let currentMonth = null;        // 보고 있는 달 "2026-11" (null이면 서버가 이번 달로 정함)

const STATUS_LABEL = {          // 서버 상태 값 → 화면 글자
    NORMAL: '정상',
    WARNING: '경고',
    EXCEEDED: '초과',
};

// ----- 표시 도우미 -----
function formatMoney(amount) {                     // 9000 → "9,000원", -1000 → "-1,000원"
    return amount.toLocaleString('ko-KR') + '원';
}

function formatMonth(ym) {                         // "2026-11" → "2026년 11월"
    const [year, month] = ym.split('-');           // "-"로 나눠 ["2026", "11"]
    return `${year}년 ${Number(month)}월`;          // Number("03") → 3 (앞의 0 제거)
}

// ----- 막대 만들기/칠하기: rate만큼 폭, status에 따라 색 -----
function paintBar(fill, rate, status) {
    fill.style.width = Math.min(rate, 100) + '%';  // 100% 넘으면 100%까지만 (막대가 틀 밖으로 안 나가게)
    fill.classList.remove('warning', 'exceeded');  // 이전 색 지우기 (다시 그릴 때를 위해)
    if (status === 'WARNING') fill.classList.add('warning');   // 경고면 주황
    if (status === 'EXCEEDED') fill.classList.add('exceeded'); // 초과면 빨강 (아니면 기본 파랑)
}

// ----- 현황 불러오기: API 호출 → 월·합계·표 그리기 -----
async function loadStatus() {
    errorMsg.textContent = '';
    try {
        const query = currentMonth ? '?month=' + currentMonth : ''; // 처음엔 조건 없이 → 이번 달
        const data = await apiFetch('/api/budgets/status' + query);
        currentMonth = data.month;                 // 서버가 실제로 적용한 달 기억 ("2026-11")
        monthLabel.textContent = formatMonth(data.month);
        renderSummary(data);
        renderRows(data.items);
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

// ----- 위쪽 합계와 전체 사용률 -----
function renderSummary(data) {
    totalBudgetEl.textContent = formatMoney(data.totalBudget);
    totalSpentEl.textContent = formatMoney(data.totalSpent);
    totalRemainingEl.textContent = formatMoney(data.totalRemaining);
    totalRemainingEl.className = data.totalRemaining < 0 ? 'amount-expense' : ''; // 잔여가 음수면 빨강

    if (data.totalUsageRate === null) {            // 예산을 하나도 안 정한 달
        paintBar(totalBar, 0, null);               // 막대 비우기
        totalRate.textContent = '설정된 예산이 없습니다';
    } else {
        paintBar(totalBar, data.totalUsageRate, null); // 전체 막대는 색 구분 없이 파랑 (상태 판정은 카테고리별로만)
        totalRate.textContent = data.totalUsageRate + '%';
    }
}

// ----- 표 그리기 -----
function renderRows(items) {
    listBody.replaceChildren();                    // 표 비우기
    if (items.length === 0) {                      // 지출 카테고리가 없으면
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 7;                            // 칸 7개를 합쳐 한 칸으로
        td.className = 'empty';
        td.textContent = '지출 카테고리가 없습니다.';
        tr.appendChild(td);
        listBody.appendChild(tr);
        return;
    }
    items.forEach(item => listBody.appendChild(createRow(item)));
}

// ----- 한 줄: [카테고리][예산][사용][잔여][막대][상태][버튼] -----
function createRow(item) {
    const tr = document.createElement('tr');
    const hasBudget = item.budget !== null;        // 예산을 정한 카테고리인지

    const nameTd = document.createElement('td');
    nameTd.textContent = item.categoryName;        // 사용자 입력값 → textContent

    const budgetTd = document.createElement('td');
    budgetTd.className = 'text-right';
    if (hasBudget) {
        budgetTd.textContent = formatMoney(item.budget);
    } else {
        budgetTd.textContent = '예산 없음';
        budgetTd.classList.add('muted');           // 흐린 글씨
    }

    const spentTd = document.createElement('td');
    spentTd.className = 'text-right';
    spentTd.textContent = formatMoney(item.spent); // 사용은 예산이 없어도 항상 숫자

    const remainingTd = document.createElement('td');
    remainingTd.className = 'text-right';
    if (hasBudget) {
        remainingTd.textContent = formatMoney(item.remaining);
        if (item.remaining < 0) remainingTd.classList.add('amount-expense'); // 초과(음수)면 빨강
    } else {
        remainingTd.textContent = '-';
    }

    const usageTd = document.createElement('td');
    if (hasBudget) {                               // 막대 + 숫자
        const usage = document.createElement('div');
        usage.className = 'usage';
        const bar = document.createElement('div');
        bar.className = 'usage-bar';
        const fill = document.createElement('div');
        fill.className = 'usage-fill';
        paintBar(fill, item.usageRate, item.status); // 사용률만큼 폭, 상태 색
        bar.appendChild(fill);
        const rate = document.createElement('span');
        rate.className = 'usage-rate';
        rate.textContent = item.usageRate + '%';   // 112%면 숫자는 그대로 112%
        usage.append(bar, rate);
        usageTd.appendChild(usage);
    } else {
        usageTd.textContent = '-';
    }

    const statusTd = document.createElement('td');
    if (item.status) {
        const span = document.createElement('span');
        span.className = 'status-' + item.status.toLowerCase(); // "WARNING" → "status-warning" (CSS 색)
        span.textContent = STATUS_LABEL[item.status];          // "경고"
        statusTd.appendChild(span);
    } else {
        statusTd.textContent = '-';
    }

    const actionTd = document.createElement('td');
    actionTd.className = 'col-actions';
    const setBtn = document.createElement('button');
    setBtn.type = 'button';
    setBtn.className = 'btn btn-sm btn-outline';
    setBtn.textContent = hasBudget ? '예산 수정' : '예산 설정'; // 동작은 6단계에서 연결
    setBtn.addEventListener('click', () => openBudgetModal(item)); // 클릭 → 이 줄의 카테고리로 모달 열기
    actionTd.appendChild(setBtn);

    tr.append(nameTd, budgetTd, spentTd, remainingTd, usageTd, statusTd, actionTd);
    return tr;
}

loadStatus();                                      // 화면이 열리면 바로 이번 달 현황
// ----- 월 이동: "2026-11"에서 diff(-1 또는 +1)만큼 옮긴 달 -----
function shiftMonth(ym, diff) {
    let [year, month] = ym.split('-').map(Number); // "2026-11" → [2026, 11] (글자 → 숫자)
    month += diff;                                 // 11 - 1 = 10, 또는 11 + 1 = 12
    if (month < 1) { month = 12; year--; }         // 1월에서 이전 → 작년 12월
    if (month > 12) { month = 1; year++; }         // 12월에서 다음 → 내년 1월
    return `${year}-${String(month).padStart(2, '0')}`; // 다시 "2026-10" 모양으로 (한 자리 월은 앞에 0)
}

document.getElementById('prev-month').addEventListener('click', () => { // ◀
    currentMonth = shiftMonth(currentMonth, -1);   // 한 달 전으로
    loadStatus();                                  // 그 달 현황 다시 불러오기
});

document.getElementById('next-month').addEventListener('click', () => { // ▶
    currentMonth = shiftMonth(currentMonth, 1);    // 한 달 뒤로
    loadStatus();
});
// ===================== 예산 설정 모달 =====================
const budgetModal = document.getElementById('budget-modal');           // 모달 전체
const budgetForm = document.getElementById('budget-form');             // 모달 form
const budgetAmount = document.getElementById('budget-amount');         // 금액 입력칸
const budgetDesc = document.getElementById('budget-modal-desc');       // "2026년 11월 · 식비"
const budgetModalError = document.getElementById('budget-modal-error');// 모달 안 에러 문구
const budgetSaveBtn = budgetForm.querySelector('button[type="submit"]'); // 저장 버튼

let editingCategoryId = null;   // 지금 예산을 정하는 카테고리 id

// ----- 모달 열기: 누른 줄의 카테고리, 지금 보는 달 -----
function openBudgetModal(item) {
    editingCategoryId = item.categoryId;                               // 어느 카테고리인지 기억
    budgetDesc.textContent = `${formatMonth(currentMonth)} · ${item.categoryName}`; // 어느 예산인지 안내
    budgetAmount.value = item.budget ?? '';                            // 예산 있으면 원래 금액, 없으면 빈칸
    budgetModalError.textContent = '';
    budgetModal.hidden = false;                                        // 보이기
    budgetAmount.focus();                                              // 바로 입력할 수 있게
}

// ----- 모달 닫기 -----
function closeBudgetModal() {
    budgetModal.hidden = true;
    editingCategoryId = null;
}

// ----- 저장: PUT /api/budgets (없으면 생성, 있으면 수정 — 서버가 알아서) -----
budgetForm.addEventListener('submit', async (event) => {
    event.preventDefault();                                            // 새로고침 막기
    budgetModalError.textContent = '';

    const body = {                                                     // BudgetRequest와 같은 이름
        categoryId: editingCategoryId,                                 // 누른 줄의 카테고리
        budgetMonth: currentMonth,                                     // 지금 보는 달 "2026-11"
        amount: budgetAmount.value === '' ? null : Number(budgetAmount.value), // 비었으면 null → 서버 문구
    };

    const before = await getUnreadNotifications();                     // 저장 전 안 읽은 알림 (api.js 함수)
    budgetSaveBtn.disabled = true;                                     // 중복 저장 방지
    try {
        await apiFetch('/api/budgets', { method: 'PUT', body: body });
        closeBudgetModal();
        await loadStatus();                                            // 현황 다시 그리기
        await showNewAlerts(before);                                   // 예산을 줄여 초과·경고가 되면 토스트 (api.js 함수)
    } catch (e) {
        budgetModalError.textContent = e.message;                      // 실패 → 모달 안에 문구, 모달은 열어 둠
    } finally {
        budgetSaveBtn.disabled = false;
    }
});

// ----- 닫는 방법: 취소, 바깥 클릭, Esc -----
document.getElementById('budget-cancel').addEventListener('click', closeBudgetModal);
budgetModal.addEventListener('click', (event) => {
    if (event.target === budgetModal) closeBudgetModal();              // 검은 막 자체를 눌렀을 때만
});
document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !budgetModal.hidden) closeBudgetModal();
});