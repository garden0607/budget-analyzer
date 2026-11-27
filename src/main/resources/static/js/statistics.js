// ===== 통계 화면 동작 (FR-13~15) =====
// 계산은 서버의 통계 API가 하고, 이 파일은 받은 숫자를 카드·차트·표로 보여 주기만 함

// ----- 화면 요소 찾기 -----
const errorMsg = document.getElementById('error-msg');       // 에러 문구 자리
const monthLabel = document.getElementById('month-label');   // "2026년 11월"
const sumIncome = document.getElementById('sum-income');     // 요약 카드: 수입
const sumExpense = document.getElementById('sum-expense');   // 요약 카드: 지출
const sumBalance = document.getElementById('sum-balance');   // 요약 카드: 잔액

// ----- 화면 상태 -----
let currentMonth = null;        // 보고 있는 달 "2026-11" (null이면 서버가 이번 달로 정함)

// ----- 표시 도우미 (예산 화면과 같은 함수) -----
function formatMoney(amount) {                     // 9000 → "9,000원"
    return amount.toLocaleString('ko-KR') + '원';
}

function formatMonth(ym) {                         // "2026-11" → "2026년 11월"
    const [year, month] = ym.split('-');
    return `${year}년 ${Number(month)}월`;
}

function shiftMonth(ym, diff) {                    // "2026-11"에서 diff(-1/+1)만큼 이동
    let [year, month] = ym.split('-').map(Number);
    month += diff;
    if (month < 1) { month = 12; year--; }         // 1월 → 작년 12월
    if (month > 12) { month = 1; year++; }         // 12월 → 내년 1월
    return `${year}-${String(month).padStart(2, '0')}`;
}

function monthQuery() {                            // 보고 있는 달을 주소 뒤 조건으로
    return currentMonth ? '?month=' + currentMonth : ''; // 처음엔 조건 없이 → 서버가 이번 달
}

// ----- ① 월 요약 (FR-13): 카드 3개 -----
async function loadSummary() {
    const data = await apiFetch('/api/statistics/summary' + monthQuery()); // { month, income, expense, balance }
    currentMonth = data.month;                     // 서버가 적용한 달 기억 → 다른 API도 같은 달로 부름
    monthLabel.textContent = formatMonth(data.month);

    sumIncome.textContent = formatMoney(data.income);
    sumExpense.textContent = formatMoney(data.expense);
    sumBalance.textContent = (data.balance >= 0 ? '+' : '-') + formatMoney(Math.abs(data.balance)); // 부호 붙이기
    sumBalance.className = data.balance >= 0 ? 'amount-income' : 'amount-expense'; // 남으면 파랑, 모자라면 빨강
}

// ----- 전체 불러오기: 요약 → 비율 차트 → 전월 대비 -----
async function loadAll() {
    errorMsg.textContent = '';
    try {
        await loadSummary();                       // 요약이 먼저 → currentMonth가 정해짐
        await loadRatio();                         // 카테고리별 비율 원형 차트
        await loadComparison();                    // 전월 대비 막대 차트 + 표
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

// ----- 월 이동 -----
document.getElementById('prev-month').addEventListener('click', () => {
    currentMonth = shiftMonth(currentMonth, -1);   // 한 달 전
    loadAll();
});
document.getElementById('next-month').addEventListener('click', () => {
    currentMonth = shiftMonth(currentMonth, 1);    // 한 달 뒤
    loadAll();
});

loadAll();                                         // 화면이 열리면 이번 달부터
// ===================== ② 카테고리별 지출 비율 (FR-14): 원형 차트 =====================
const ratioTotal = document.getElementById('ratio-total');        // "총 지출 18,000원"
const ratioCanvas = document.getElementById('ratio-chart');       // 원형 차트 도화지
const ratioEmpty = document.getElementById('ratio-empty');        // 지출 0원일 때 안내

const COLORS = ['#2563eb', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16']; // 조각 색 8개 (넘으면 처음부터 반복)

let ratioChart = null;          // 지금 그려진 원형 차트 (다시 그릴 때 지우려고 기억)

async function loadRatio() {
    const data = await apiFetch('/api/statistics/category-ratio' + monthQuery()); // { totalExpense, items(금액 큰 순) }

    if (ratioChart) {                              // 이전 달 차트가 있으면
        ratioChart.destroy();                      // 먼저 지우기 (같은 도화지에 두 번 못 그림)
        ratioChart = null;
    }

    const empty = data.totalExpense === 0;         // 이번 달 지출이 없으면
    ratioCanvas.parentElement.hidden = empty;      // 차트 상자 숨기고
    ratioEmpty.hidden = !empty;                    // "지출 내역이 없습니다" 보이기
    ratioTotal.textContent = '총 지출 ' + formatMoney(data.totalExpense);
    if (empty) return;                             // 그릴 게 없으니 끝

    ratioChart = new Chart(ratioCanvas, {          // 원형 차트 그리기
        type: 'doughnut',                          // 가운데가 빈 원형
        data: {
            labels: data.items.map(i => i.categoryName),           // 조각 이름: ["식비", "교통비"]
            datasets: [{
                data: data.items.map(i => i.amount),               // 조각 크기: [9000, 9000]
                backgroundColor: data.items.map((_, idx) => COLORS[idx % COLORS.length]), // 조각마다 색 (8개 넘으면 반복)
            }],
        },
        options: {
            maintainAspectRatio: false,            // 정사각형 고집하지 말고 .chart-box 높이(260px)에 맞추기
            plugins: {
                legend: { position: 'right' },     // 범례(색·이름 목록)를 오른쪽에
                tooltip: {
                    callbacks: {                   // 마우스 올렸을 때 글자 직접 정하기
                        label: (ctx) => {
                            const item = data.items[ctx.dataIndex];             // 마우스가 올라간 조각의 원래 데이터
                            return ` ${formatMoney(item.amount)} (${item.ratio.toFixed(1)}%)`; // " 9,000원 (50.0%)"
                        },
                    },
                },
            },
        },
    });
}
// ===================== ③ 전월 대비 (FR-15): 막대 차트 + 표 =====================
const compareCanvas = document.getElementById('compare-chart');   // 막대 차트 도화지
const compareList = document.getElementById('compare-list');      // 표 몸통
const colLast = document.getElementById('col-last');              // 표 제목 "지난달" 칸
const colThis = document.getElementById('col-this');              // 표 제목 "이번 달" 칸

let compareChart = null;        // 지금 그려진 막대 차트 (다시 그릴 때 지우려고 기억)

async function loadComparison() {
    const data = await apiFetch('/api/statistics/comparison' + monthQuery()); // { month, previousMonth, items }

    colLast.textContent = formatMonth(data.previousMonth);         // 표 제목을 "2026년 10월"로
    colThis.textContent = formatMonth(data.month);                 // "2026년 11월"로

    if (compareChart) {                            // 이전 차트 지우기 (원형 차트와 같은 이유)
        compareChart.destroy();
        compareChart = null;
    }

    const empty = data.items.length === 0;         // 두 달 모두 지출이 없으면
    compareCanvas.parentElement.hidden = empty;    // 차트 상자 숨기기
    renderCompareRows(data.items);                 // 표는 항상 그림 (비었으면 안내 줄)
    if (empty) return;

    compareChart = new Chart(compareCanvas, {
        type: 'bar',                               // 막대 차트
        data: {
            labels: data.items.map(i => i.categoryName),                // 가로축: 카테고리 이름
            datasets: [
                {
                    label: formatMonth(data.previousMonth),             // 범례: "2026년 10월"
                    data: data.items.map(i => i.lastMonth),             // 지난달 금액
                    backgroundColor: '#cbd5e1',                         // 회색
                },
                {
                    label: formatMonth(data.month),                     // 범례: "2026년 11월"
                    data: data.items.map(i => i.thisMonth),             // 이번 달 금액
                    backgroundColor: '#2563eb',                         // 파랑
                },
            ],
        },
        options: {
            maintainAspectRatio: false,            // .chart-box 높이(260px)에 맞추기
            scales: {
                y: {
                    beginAtZero: true,             // 세로축을 0부터 (안 그러면 작은 차이가 크게 보임)
                    ticks: { callback: (value) => value.toLocaleString('ko-KR') }, // 세로축 숫자에 쉼표
                },
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: (ctx) => ` ${ctx.dataset.label}: ${formatMoney(ctx.parsed.y)}`, // " 2026년 11월: 9,000원"
                    },
                },
            },
        },
    });
}

// ----- 표 그리기: [카테고리][지난달][이번 달][증감][증감률] -----
function renderCompareRows(items) {
    compareList.replaceChildren();                 // 표 비우기
    if (items.length === 0) {                      // 비교할 지출이 없으면 안내 한 줄
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 5;
        td.className = 'empty';
        td.textContent = '두 달 모두 지출 내역이 없습니다.';
        tr.appendChild(td);
        compareList.appendChild(tr);
        return;
    }

    items.forEach(i => {
        const tr = document.createElement('tr');

        const nameTd = document.createElement('td');
        nameTd.textContent = i.categoryName;                          // 사용자 입력값 → textContent

        const lastTd = document.createElement('td');
        lastTd.className = 'text-right';
        lastTd.textContent = formatMoney(i.lastMonth);

        const thisTd = document.createElement('td');
        thisTd.className = 'text-right';
        thisTd.textContent = formatMoney(i.thisMonth);

        const diffTd = document.createElement('td');
        diffTd.className = 'text-right';
        if (i.diff > 0) {                                             // 지출 증가 → 빨강 +
            diffTd.textContent = '+' + formatMoney(i.diff);
            diffTd.classList.add('amount-expense');
        } else if (i.diff < 0) {                                      // 지출 감소 → 파랑 −
            diffTd.textContent = '-' + formatMoney(Math.abs(i.diff));
            diffTd.classList.add('amount-income');
        } else {                                                      // 변화 없음
            diffTd.textContent = '0원';
            diffTd.classList.add('muted');
        }

        const rateTd = document.createElement('td');
        rateTd.className = 'text-right';
        if (i.changeRate === null) {                                  // 지난달 0원 → 비율 계산 불가
            const tag = document.createElement('span');
            tag.className = 'tag';                                    // 카테고리 화면의 "기본" 꼬리표 모양 재사용
            tag.textContent = '신규';
            rateTd.appendChild(tag);
        } else {
            const sign = i.changeRate > 0 ? '+' : '';                 // 음수는 이미 -가 붙어 있음
            rateTd.textContent = sign + i.changeRate.toFixed(1) + '%'; // "-25.0%", "+50.0%"
        }

        tr.append(nameTd, lastTd, thisTd, diffTd, rateTd);
        compareList.appendChild(tr);
    });
}
// ===================== ④ 정기 지출 자동 탐지 결과 =====================
const recurringList = document.getElementById('recurring-list');    // 목록 (ul)
const recurringTotal = document.getElementById('recurring-total');  // "매달 약 83,400원"

// 판단 이유 문장 만들기: 서버가 준 재료를 조합
function buildReason(item) {
    let reason = `최근 ${item.months}개월 연속, 매달 ${item.dayOfMonth}일쯤 결제`; // 기본 이유
    if (item.previousAmount !== null) {                         // 금액이 바뀐 적이 있으면
        const word = item.amount > item.previousAmount ? '인상' : '인하';       // 올랐는지 내렸는지
        reason += ` (${formatMonth(item.changedMonth)}에 ${formatMoney(item.previousAmount)} → ${formatMoney(item.amount)} ${word})`;
    }
    return reason;
}

async function loadRecurring() {
    try {
        const data = await apiFetch('/api/statistics/recurring'); // { items, monthlyTotal } (항상 오늘 기준)
        recurringList.replaceChildren();

        if (data.items.length === 0) {                          // 아직 탐지된 게 없으면
            recurringTotal.textContent = '';
            const li = document.createElement('li');
            li.className = 'muted';
            li.textContent = '아직 매달 반복되는 지출이 없습니다.';
            recurringList.appendChild(li);
            return;
        }

        recurringTotal.textContent = `매달 약 ${formatMoney(data.monthlyTotal)}`;

        data.items.forEach(item => {
            const li = document.createElement('li');
            li.className = 'recurring-item';

            const head = document.createElement('div');         // 윗줄: 이름 · 카테고리 ... 금액
            head.className = 'recurring-head';
            const left = document.createElement('span');
            const name = document.createElement('span');
            name.className = 'recurring-name';
            name.textContent = item.name;                       // 사용자 입력값(메모) → textContent
            const category = document.createElement('span');
            category.className = 'tag';                         // 작은 꼬리표 (카테고리 화면에서 만든 CSS)
            category.textContent = item.categoryName;
            left.append(name, category);
            const amount = document.createElement('strong');
            amount.className = 'amount-expense';
            amount.textContent = formatMoney(item.amount);
            head.append(left, amount);

            const reason = document.createElement('div');       // 아랫줄: 판단 이유
            reason.className = 'recurring-reason';
            reason.textContent = buildReason(item);

            li.append(head, reason);
            recurringList.appendChild(li);
        });
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

loadRecurring();                                                // 화면이 열릴 때 한 번만 (달을 바꿔도 다시 안 부름)