// ===== 로그인 화면(S-01) 동작 =====
// getToken, saveToken, apiFetch는 api.js에 있는 함수. head에서 api.js를 먼저 불러와서 여기서 쓸 수 있음

if (getToken()) {                                          // 이미 토큰이 있으면 (로그인한 상태)
    location.href = '/';                                   // 로그인 화면을 보여 줄 필요 없이 홈으로
}

const form = document.getElementById('login-form');        // login.html의 form 태그 찾기
const errorMsg = document.getElementById('error-msg');     // 에러 문구 넣을 자리 찾기
const submitBtn = form.querySelector('button[type="submit"]'); // form 안의 로그인 버튼 찾기

form.addEventListener('submit', async (event) => {         // form이 제출되면 (버튼 클릭 또는 Enter)
    event.preventDefault();                                // 페이지 새로고침(기본 동작) 막기
    errorMsg.textContent = '';                             // 이전 에러 문구 지우기

    const email = document.getElementById('email').value.trim(); // 이메일 입력값 (앞뒤 공백 제거)
    const password = document.getElementById('password').value;  // 비밀번호 입력값 (공백도 비밀번호일 수 있어 그대로)

    if (!email || !password) {                             // 둘 중 하나라도 비어 있으면
        errorMsg.textContent = '이메일과 비밀번호를 입력해 주세요.'; // 서버까지 안 보내고 바로 안내
        return;                                            // 여기서 멈춤
    }

    submitBtn.disabled = true;                             // 응답 올 때까지 버튼 잠금 (여러 번 클릭 방지)
    try {
        const data = await apiFetch('/api/auth/login', {   // 로그인 API 호출하고 응답 기다림
            method: 'POST',                                // 로그인은 POST
            body: { email: email, password: password },    // Swagger에서 보낸 JSON과 같은 모양
        });
        saveToken(data.accessToken);                       // 응답의 토큰 저장 (필드 이름 확인 필요, 아래 참고)
        location.href = '/';                               // 홈으로 이동
    } catch (e) {                                          // 실패 (비밀번호 틀림 등)
        errorMsg.textContent = e.message;                  // 서버가 준 message를 빨간 글씨로 표시
    } finally {
        submitBtn.disabled = false;                        // 성공이든 실패든 버튼 다시 풀기
    }
});