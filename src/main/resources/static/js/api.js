// ===== 공통 JS: 모든 화면이 같이 쓰는 토큰 관리 · API 호출 · 헤더 동작 =====

const TOKEN_KEY = 'accessToken';                        // localStorage에 토큰을 저장할 이름(열쇠)

// ----- 1. 토큰 보관 -----
function getToken() {                                   // 저장된 토큰 꺼내기 (없으면 null)
    return localStorage.getItem(TOKEN_KEY);
}
function saveToken(token) {                             // 로그인 성공 시 토큰 저장 (3일차 로그인 화면에서 사용)
    localStorage.setItem(TOKEN_KEY, token);
}
function clearToken() {                                 // 로그아웃·만료 시 토큰 삭제
    localStorage.removeItem(TOKEN_KEY);
}

// ----- 2. API 호출: 토큰 자동 첨부 + 에러 처리 -----
// 사용 예) const data = await apiFetch('/api/categories');
//         await apiFetch('/api/categories', { method: 'POST', body: { name: '카페', type: 'EXPENSE' } });
async function apiFetch(url, options = {}) {
    const token = getToken();                           // 지금 저장된 토큰
    const headers = { ...(options.headers || {}) };     // 호출한 쪽이 준 헤더가 있으면 복사해서 시작

    if (options.body !== undefined) {                   // 보낼 데이터가 있으면
        headers['Content-Type'] = 'application/json';   // "JSON으로 보낸다"고 서버에 알림 (@RequestBody가 읽을 수 있게)
    }
    if (token) {                                        // 토큰이 있으면
        headers['Authorization'] = 'Bearer ' + token;   // Swagger Authorize와 같은 헤더를 자동으로 붙임
    }

    const response = await fetch(url, {                 // 서버에 요청 보내고 응답 올 때까지 기다림
        ...options,                                     // method(GET/POST/PUT/DELETE 등) 그대로 사용
        headers,
        body: options.body !== undefined ? JSON.stringify(options.body) : undefined, // JS 객체 → JSON 글자로 변환
    });

    if (response.status === 401 && token) {            // 토큰을 보냈는데 401 = 토큰 만료·위조
        clearToken();                                   // 쓸모없는 토큰 삭제
        location.href = '/login';                       // 로그인 화면으로 이동
        throw new Error('로그인이 만료되었습니다.');       // 호출한 쪽 코드가 더 진행하지 않게 멈춤
    }

    if (response.status === 204) {                      // 204 No Content (예: 삭제 성공) = 응답 내용 없음
        return null;
    }

    const data = await response.json().catch(() => null); // 응답 JSON → JS 객체 (내용이 없거나 깨졌으면 null)

    if (!response.ok) {                                 // 200번대가 아니면 실패 (400, 404, 409 등)
        const error = new Error(data?.message || '요청을 처리하지 못했습니다.'); // 서버가 준 {code, message}의 message 사용
        error.code = data?.code;                        // 에러 코드도 같이 담아 둠 (예: DUPLICATE_EMAIL)
        error.status = response.status;                 // HTTP 상태 번호
        throw error;                                    // 호출한 쪽에서 try/catch로 받아 화면에 표시
    }

    return data;                                        // 성공: 응답 데이터 돌려줌
}

// ----- 3. 로그인 확인 -----
function requireLogin() {                               // 로그인이 필요한 화면에서 사용
    if (!getToken()) {                                  // 토큰이 없으면 (로그인 안 함)
        location.href = '/login';                       // 바로 로그인 화면으로
        return false;
    }
    return true;
}

// ----- 4. 헤더(C-01) 동작: 헤더가 있는 화면에서만 실행 -----
async function loadUnreadCount() {                      // 🔔 옆 안 읽은 알림 개수 채우기
    const badge = document.getElementById('unread-count'); // layout.html의 id="unread-count" 태그 찾기
    try {
        const data = await apiFetch('/api/notifications?unreadOnly=true'); // 10주차에 만든 알림 API
        const count = data.unreadCount;                 // 응답의 안 읽은 개수 (필드 이름 확인 필요, 아래 참고)
        badge.textContent = count;                      // 숫자 넣기
        badge.hidden = count === 0;                     // 0이면 숨기고, 1 이상이면 보이기
    } catch (e) {
        badge.hidden = true;                            // 실패해도 화면 전체가 멈추지 않게 숫자만 숨김
    }
}

function initHeader() {
    const logoutBtn = document.getElementById('logout-btn'); // 헤더의 로그아웃 버튼
    if (!logoutBtn) return;                             // 헤더 없는 화면(로그인·회원가입)이면 아무것도 안 함
    if (!requireLogin()) return;                        // 헤더 있는 화면 = 로그인 필요 → 토큰 없으면 이동

    logoutBtn.addEventListener('click', () => {         // 로그아웃 버튼을 누르면
        clearToken();                                   // 토큰 삭제 (JWT는 서버에 저장된 게 없어서 이걸로 끝)
        location.href = '/login';                       // 로그인 화면으로
    });

    loadUnreadCount();                                  // 🔔 숫자 채우기
}

initHeader();                                           // 파일이 실행되면 바로 헤더 동작 연결 (defer라 HTML이 다 그려진 뒤)
// ----- 5. 새 예산 알림 표시: 저장 전후 안 읽은 알림을 비교해 토스트로 (FR-05, 거래·예산 화면 공통) -----

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