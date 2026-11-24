// ===== 회원가입 화면(S-02) 동작 =====
// getToken, apiFetch는 api.js에 있는 함수 (head에서 먼저 불러옴)

if (getToken()) {                                          // 이미 로그인한 상태면
    location.href = '/';                                   // 가입 화면 대신 홈으로
}

const form = document.getElementById('signup-form');       // signup.html의 form 찾기
const errorMsg = document.getElementById('error-msg');     // 에러 문구 자리 찾기
const submitBtn = form.querySelector('button[type="submit"]'); // 가입하기 버튼 찾기

form.addEventListener('submit', async (event) => {         // form 제출 시 (버튼 클릭 또는 Enter)
    event.preventDefault();                                // 페이지 새로고침 막기
    errorMsg.textContent = '';                             // 이전 에러 문구 지우기

    const body = {                                         // 서버로 보낼 값 (SignupRequest 필드 이름과 똑같이)
        email: document.getElementById('email').value.trim(),       // 이메일 (앞뒤 공백 제거)
        password: document.getElementById('password').value,         // 비밀번호 (그대로)
        passwordConfirm: document.getElementById('passwordConfirm').value, // 비밀번호 확인
        nickname: document.getElementById('nickname').value.trim(),  // 닉네임 (앞뒤 공백 제거)
    };

    submitBtn.disabled = true;                             // 응답 올 때까지 버튼 잠금 (중복 가입 요청 방지)
    try {
        await apiFetch('/api/auth/signup', {               // 회원가입 API 호출 (주소 확인 필요, 아래 참고)
            method: 'POST',                                // 새 계정을 만드는 거라 POST
            body: body,                                    // 위에서 만든 4개 값
        });
        alert('회원가입이 완료되었습니다. 로그인해 주세요.'); // 성공 안내 (확인을 누를 때까지 대기)
        location.href = '/login';                          // 로그인 화면으로 이동
    } catch (e) {                                          // 실패 (형식 오류 400, 비밀번호 불일치, 이메일 중복 409 등)
        errorMsg.textContent = e.message;                  // 서버가 준 문구를 그대로 표시
    } finally {
        submitBtn.disabled = false;                        // 버튼 다시 풀기
    }
});