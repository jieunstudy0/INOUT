document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            try {
                const response = await fetch('/api/user/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const result = await response.json();
                console.log("📌 서버 응답 전체:", result);

                // 1. 성공 여부 판단 (ResponseResult 규격 활용)
                // result.body가 존재하면 성공으로 간주
                if (response.ok && result.body) {
                    
                    const data = result.body; // body 안의 데이터 추출

                    // 2. 토큰 및 권한 추출 (서버에서 보낸 키값 'role', 'accessToken'과 정확히 일치해야 함)
                    const accessToken = data.accessToken;
                    const userRole = data.role; // Java에서 "role"로 보냈으므로 여기서도 role로 받음

                    console.log(`✅ 토큰: ${accessToken ? "있음" : "없음"}, 권한: ${userRole}`);

                    if (accessToken) {
                        // 3. 쿠키 저장 (가장 중요)
                        // 주의: Java JWT Provider에서 "Bearer " 없이 토큰만 줬다면 그대로 저장
                        // 만약 토큰 문자열 자체에 "Bearer "가 없다면 substring 필요 없음
                        const tokenValue = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
                        
                        // path=/ 설정 필수 (모든 페이지에서 쿠키 접근 가능하도록)
                        document.cookie = `accessToken=${tokenValue}; path=/; max-age=3600;`; 
                    }

                    // 4. 페이지 이동
                    if (userRole === "ROLE_ADMIN") {
                        alert("관리자님 환영합니다.");
                        location.href = "/admin/dashboard";
                    } else if (userRole === "ROLE_USER" || userRole === "ROLE_EMPLOYEE") {
                        alert("로그인되었습니다.");
                        location.href = "/stock/emp/list"; // 혹은 /user/main
                    } else {
                        console.error("알 수 없는 권한:", userRole);
                        alert("권한 정보를 확인할 수 없습니다.");
                    }

                } else {
                    // 실패 처리
                    const msg = result.message || "로그인에 실패했습니다.";
                    alert(msg);
                }

            } catch (error) {
                console.error("로그인 시스템 에러:", error);
                alert("서버와 통신 중 오류가 발생했습니다.");
            }
        });
    }
});