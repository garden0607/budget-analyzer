// ===== 대시보드(홈, S-03) 동작 =====
// 새 API 없이, 이미 만든 통계·예산·거래 API를 모아서 이번 달 상황을 보여 줌

// ----- 화면 요소 찾기 -----
const errorMsg = document.getElementById('error-msg');
const dashTitle = document.getElementById('dash-title');           // "2026년 11월 요약"
const sumIncome = document.getElementById('sum-income');
const sumExpense = document.getElementById('sum-expense');
const sumBalance = document.getElementById('sum-balance');
const totalBar = document.getElementById('dash-total-bar');        // 전체 사용률 막대
const totalRate = document.getElementById('dash-total-rate');      // 전체 사용률 숫자
const budgetList = document.getElementById('dash-budgets');        // 예산 목록 (ul)
const txBody = document.getElementById('dash-transactions');       // 최근 거래 표 몸통

const STATUS_LABEL = { NORMAL: '정상', WARNING: '경고', EXCEEDED: '초과' }; // 서버 상태 → 화면 글자

// ----- 표시 도우미 (다른 화면과 같은 함수) -----
function formatMoney(amount) {                     // 9000 → "9,000원"
    return amount.toLocaleString('ko-KR') + '원';
}

function formatMonth(ym) {                         // "2026-11" → "2026년 11월"
    const [year, month] = ym.split('-');
    return `${year}년 ${Number(month)}월`;
}

function paintBar(fill, rate, status) {            // 막대 폭·색 (예산 화면과 같음)
    fill.style.width = Math.min(rate, 100) + '%';
    fill.classList.remove('warning', 'exceeded');
    if (status === 'WARNING') fill.classList.add('warning');
    if (status === 'EXCEEDED') fill.classList.add('exceeded');
}

function emptyItem(text) {                         // 목록이 비었을 때 안내 한 줄 (li)
    const li = document.createElement('li');
    li.className = 'muted';
    li.textContent = text;
    return li;
}

// ----- 전체 불러오기: 세 API를 동시에 -----
async function loadDashboard() {
    errorMsg.textContent = '';
    try {
        const [summary, budget, tx] = await Promise.all([   // 동시에 보내고 셋 다 올 때까지 기다림
            apiFetch('/api/statistics/summary'),            // 이번 달 요약
            apiFetch('/api/budgets/status'),                // 이번 달 예산 현황
            apiFetch('/api/transactions'),                  // 이번 달 거래 첫 페이지 (최신순)
        ]);
        renderSummary(summary);
        renderBudgets(budget);
        renderTransactions(tx.transactions.slice(0, 5));    // 앞에서 5건만
    } catch (e) {
        errorMsg.textContent = e.message;                   // 하나라도 실패하면 문구 표시
    }
}

// ----- ① 요약 카드 -----
function renderSummary(data) {
    dashTitle.textContent = `${formatMonth(data.month)} 요약`; // "2026년 11월 요약"
    sumIncome.textContent = formatMoney(data.income);
    sumExpense.textContent = formatMoney(data.expense);
    sumBalance.textContent = (data.balance >= 0 ? '+' : '-') + formatMoney(Math.abs(data.balance));
    sumBalance.className = data.balance >= 0 ? 'amount-income' : 'amount-expense';
}

// ----- ② 예산 현황: 전체 막대 + 사용률 높은 순 5개 -----
function renderBudgets(data) {
    if (data.totalUsageRate === null) {            // 이번 달 예산이 하나도 없으면
        paintBar(totalBar, 0, null);
        totalRate.textContent = '-';
    } else {
        paintBar(totalBar, data.totalUsageRate, null); // 전체 막대는 색 구분 없이 (예산 화면과 같은 이유)
        totalRate.textContent = data.totalUsageRate + '%';
    }

    budgetList.replaceChildren();
    const withBudget = data.items
        .filter(i => i.budget !== null)            // 예산을 정한 카테고리만
        .sort((a, b) => b.usageRate - a.usageRate) // 사용률 높은 순 (위험한 것부터 보이게)
        .slice(0, 5);                              // 5개까지

    if (withBudget.length === 0) {
        budgetList.appendChild(emptyItem('설정된 예산이 없습니다. 예산 관리에서 정해 보세요.'));
        return;
    }

    withBudget.forEach(i => {
        const li = document.createElement('li');
        const usage = document.createElement('div');
        usage.className = 'usage';

        const name = document.createElement('span');
        name.textContent = i.categoryName;         // 사용자 입력값 → textContent

        const bar = document.createElement('div');
        bar.className = 'usage-bar';
        const fill = document.createElement('div');
        fill.className = 'usage-fill';
        paintBar(fill, i.usageRate, i.status);     // 상태 색
        bar.appendChild(fill);

        const rate = document.createElement('span');
        rate.className = 'usage-rate status-' + i.status.toLowerCase(); // 숫자도 상태 색 (status-warning 등)
        rate.textContent = `${i.usageRate}% ${STATUS_LABEL[i.status]}`;  // "112% 초과"
        rate.style.minWidth = '80px';              // "112% 초과"가 한 줄에 들어가게

        usage.append(name, bar, rate);
        li.appendChild(usage);
        budgetList.appendChild(li);
    });
}

// ----- ③ 최근 거래 5건 -----
function renderTransactions(list) {
    txBody.replaceChildren();
    if (list.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.className = 'empty';
        td.textContent = '이번 달 거래가 없습니다.';
        tr.appendChild(td);
        txBody.appendChild(tr);
        return;
    }

    list.forEach(t => {
        const tr = document.createElement('tr');

        const dateTd = document.createElement('td');
        dateTd.textContent = t.transactionDate.slice(5);           // "2026-11-25" → "11-25" (짧게)
        dateTd.className = 'muted';

        const nameTd = document.createElement('td');
        nameTd.textContent = t.memo ? `${t.categoryName} · ${t.memo}` : t.categoryName; // "식비 · 점심"

        const amountTd = document.createElement('td');
        const isIncome = t.type === 'INCOME';
        amountTd.textContent = (isIncome ? '+' : '-') + formatMoney(t.amount);
        amountTd.className = 'text-right ' + (isIncome ? 'amount-income' : 'amount-expense');

        tr.append(dateTd, nameTd, amountTd);
        txBody.appendChild(tr);
    });
}

loadDashboard();                                   // 화면이 열리면 바로