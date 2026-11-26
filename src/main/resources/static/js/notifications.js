// ===== 알림 화면 동작 =====
// apiFetch, loadUnreadCount는 api.js의 함수

// ----- 화면 요소 찾기 -----
const listEl = document.getElementById('notification-list');      // 알림 목록 (ul)
const errorMsg = document.getElementById('error-msg');            // 에러 문구 자리
const unreadTotalEl = document.getElementById('unread-total');    // "안 읽은 알림 N개"의 숫자
const filterAllBtn = document.getElementById('filter-all');       // [전체]
const filterUnreadBtn = document.getElementById('filter-unread'); // [안 읽은 것만]

// ----- 화면 상태 -----
let unreadOnly = false;          // false면 전체, true면 안 읽은 것만

const TYPE_LABEL = {             // 서버 값 → 화면 글자
    WARNING: '경고',
    EXCEEDED: '초과',
};

// ----- 시각 표시: "2026-11-26T16:29:05.123" → "2026-11-26 16:29" -----
function formatDateTime(iso) {
    return iso.slice(0, 16).replace('T', ' '); // 앞 16글자(분까지)만 자르고 T를 띄어쓰기로
}

// ----- 목록 불러오기 -----
async function loadNotifications() {
    errorMsg.textContent = '';
    try {
        const query = unreadOnly ? '?unreadOnly=true' : '';       // 안 읽은 것만이면 조건 붙이기
        const data = await apiFetch('/api/notifications' + query); // { unreadCount, notifications(최신순) }
        unreadTotalEl.textContent = data.unreadCount;              // 안 읽은 개수 (보기 전환과 상관없이 항상 전체 기준)
        renderList(data.notifications);
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

// ----- 목록 그리기 -----
function renderList(notifications) {
    listEl.replaceChildren();                                      // 목록 비우기
    if (notifications.length === 0) {                              // 알림이 없으면 안내 한 줄
        const li = document.createElement('li');
        li.className = 'notice-item muted';
        li.textContent = unreadOnly ? '안 읽은 알림이 없습니다.' : '알림이 없습니다.';
        listEl.appendChild(li);
        return;
    }
    notifications.forEach(n => listEl.appendChild(createItem(n)));
}

// ----- 알림 한 줄: [●] [초과] 문구 ... 시각 -----
function createItem(n) {
    const li = document.createElement('li');
    li.className = 'notice-item ' + (n.isRead ? 'read' : 'unread'); // 읽음/안 읽음에 따라 모양 (CSS)

    const dot = document.createElement('span');
    dot.className = 'notice-dot';                                  // 안 읽음 점 (읽었으면 CSS가 숨김)

    const type = document.createElement('span');
    type.className = 'notice-type ' + n.type.toLowerCase();        // "WARNING" → "warning" (주황), "exceeded" (빨강)
    type.textContent = TYPE_LABEL[n.type];                         // "경고" / "초과"

    const message = document.createElement('span');
    message.className = 'notice-message';
    message.textContent = n.message;                               // 서버가 만든 문구 (textContent로 안전하게)

    const time = document.createElement('span');
    time.className = 'notice-time';
    time.textContent = formatDateTime(n.createdAt);                // "2026-11-26 16:29"

    li.append(dot, type, message, time);

    if (!n.isRead) {                                               // 안 읽은 알림만 클릭하면 읽음 처리
        li.addEventListener('click', () => markAsRead(n.id));
    }
    return li;
}

// ----- 읽음 처리: PATCH → 목록 다시 그리기 → 헤더 🔔 숫자 갱신 -----
async function markAsRead(id) {
    errorMsg.textContent = '';
    try {
        await apiFetch(`/api/notifications/${id}/read`, { method: 'PATCH' }); // 보낼 내용 없이 주소로만
        await loadNotifications();                                 // 목록·개수 다시 그리기
        loadUnreadCount();                                         // api.js 함수: 헤더 🔔 숫자도 줄이기
    } catch (e) {
        errorMsg.textContent = e.message;
    }
}

// ----- 보기 전환: 버튼 색 바꾸고 다시 조회 -----
function setFilter(onlyUnread) {
    unreadOnly = onlyUnread;                                       // 상태 바꾸기
    filterAllBtn.classList.toggle('active', !onlyUnread);          // 전체면 [전체]에 active
    filterUnreadBtn.classList.toggle('active', onlyUnread);        // 안 읽은 것만이면 그쪽에 active
    loadNotifications();
}

filterAllBtn.addEventListener('click', () => setFilter(false));
filterUnreadBtn.addEventListener('click', () => setFilter(true));

loadNotifications();                                               // 화면이 열리면 전체 알림부터