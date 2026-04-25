document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            // 에러 메시지 div 가져오기 (HTML에 추가한 요소)
            const errorDiv = document.getElementById('errorMessage');

            // 제출할 때마다 기존 에러 메시지 숨기기
            errorDiv.style.display = 'none';

            try {
                const response = await fetch('/api/user/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const result = await response.json();
                console.log("서버 응답 전체:", result);

                if (response.ok && result.header && result.header.result === true) {

                    const data = result.body;

                    const accessToken  = data.accessToken;
                    const refreshToken = data.refreshToken;
                    const userRole     = data.role;

                    console.log(`토큰: ${accessToken ? "있음" : "없음"}, 권한: ${userRole}`);

                    if (accessToken) {
                        document.cookie = `accessToken=${accessToken}; path=/; max-age=3600;`;
                    }

                    if (refreshToken) {
                        localStorage.setItem('refreshToken', refreshToken);
                    }

                    if (userRole === 'ROLE_ADMIN') {
                        location.href = "/admin/dashboard";

                    } else if (userRole === 'ROLE_EMPLOYEE') {
                        location.href = "/stock/emp/list";

                    } else {
                        console.error("알 수 없는 권한:", userRole);
                  
                        errorDiv.textContent = "권한 정보를 확인할 수 없습니다. 관리자에게 문의해주세요.";
                        errorDiv.style.display = 'block';
                    }

                } else {
                    // 기존 alert(errorMsg) → errorDiv로 교체
                    const errorMsg = result.message
                        || (result.header && result.header.message)
                        || "로그인에 실패했습니다.";

                    errorDiv.textContent = errorMsg;   // ← 메시지 내용 설정
                    errorDiv.style.display = 'block';  // ← 화면에 표시
                }

            } catch (error) {
                console.error("로그인 시스템 에러:", error);
                errorDiv.textContent = "서버와 통신 중 오류가 발생했습니다.";
                errorDiv.style.display = 'block';
            }
        });
    }
});