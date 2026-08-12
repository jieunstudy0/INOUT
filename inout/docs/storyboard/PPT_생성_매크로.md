# INOUT AS-IS 스토리보드 → PPT 자동 생성 VBA 매크로

`AS-IS_스토리보드.md` 내용을 파워포인트 슬라이드로 자동 생성하는 매크로입니다. 총 **56장**을 **10장 단위 6개 파트(Part0 공통 + Part1~6)** 로 나누어 제공합니다.

## 사용 방법

1. 빈 PowerPoint 파일을 새로 만들고 저장합니다. (레이아웃은 자동으로 처리되므로 슬라이드는 비워둬도 됩니다.)
2. 리본 메뉴 `개발 도구(Developer) → Visual Basic` 클릭 (Developer 탭이 없다면 `파일 → 옵션 → 리본 사용자 지정`에서 활성화)
3. VBA 편집기에서 `삽입(Insert) → 모듈(Module)` 로 새 모듈을 만듭니다.
4. 아래 **[공통 모듈: Part0]** 코드를 먼저 붙여넣습니다. (헬퍼 함수 + 실행용 마스터 매크로 포함)
5. 이어서 **Part1 ~ Part6** 코드를 같은 모듈(또는 각각 새 모듈)에 순서대로 이어 붙여넣습니다.
6. `F5` 또는 매크로 실행 창에서 `CreateStoryboardPPT` 를 실행하면 56장의 슬라이드가 순서대로 자동 생성됩니다.
   - 슬라이드가 너무 많아 한 번에 부담스러우면, `CreateStoryboardPPT` 대신 `CreateSlides_Part1`, `CreateSlides_Part2` ... 를 순서대로 하나씩 실행해도 됩니다.

> 실행 전 반드시 **빈 프레젠테이션 상태**에서 실행하는 것을 권장합니다. (기존 슬라이드 뒤에 이어서 추가되는 방식이라, 여러 번 실행하면 중복 생성됩니다.)

---

## [공통 모듈: Part0] 헬퍼 함수 + 마스터 실행 매크로

```vba
Option Explicit

' ======================================================
' INOUT AS-IS 스토리보드 자동 생성 매크로 - 공통 모듈
' ======================================================

' 전체 6개 파트를 순서대로 실행하여 56장 슬라이드를 한 번에 생성합니다.
Sub CreateStoryboardPPT()
    Dim startTime As Double
    startTime = Timer

    CreateSlides_Part1   ' 0~3장: 표지/IA/공통/로그인 앞부분
    CreateSlides_Part2   ' 로그인 뒷부분/대시보드/재고관리 앞부분
    CreateSlides_Part3   ' 재고관리 뒷부분/발주관리
    CreateSlides_Part4   ' 배송/예치금/직원관리 앞부분
    CreateSlides_Part5   ' 직원관리 뒷부분/문의사항/연차관리
    CreateSlides_Part6   ' AI자동발주/원본대비 총정리/마무리

    MsgBox "전체 " & ActivePresentation.Slides.Count & "장의 슬라이드 생성이 완료되었습니다." & vbCrLf & _
           "소요 시간: " & Format(Timer - startTime, "0.0") & "초", vbInformation, "INOUT 스토리보드 생성 완료"
End Sub

' 챕터 구분용 대표지(Section Divider) 슬라이드를 추가합니다. (제목만 큼직하게 배치)
Sub AddSectionSlide(ByVal chapterTitle As String, ByVal subtitle As String)
    Dim sld As Slide
    Dim idx As Integer
    idx = ActivePresentation.Slides.Count + 1
    Set sld = ActivePresentation.Slides.Add(idx, ppLayoutTitle)

    With sld.Shapes.Title.TextFrame.TextRange
        .Text = chapterTitle
        .Font.Size = 40
        .Font.Bold = True
    End With

    If sld.Shapes.Placeholders.Count >= 2 Then
        With sld.Shapes.Placeholders(2).TextFrame.TextRange
            .Text = subtitle
            .Font.Size = 20
        End With
    End If
End Sub

' 일반 상세 슬라이드를 추가합니다. (제목 + [화면]/[데이터]/[링크] 본문)
Sub AddSlide(ByVal slideTitle As String, ByVal bodyText As String)
    Dim sld As Slide
    Dim idx As Integer
    idx = ActivePresentation.Slides.Count + 1

    ' ppLayoutText = 2 (제목 + 본문 텍스트 레이아웃)
    Set sld = ActivePresentation.Slides.Add(idx, ppLayoutText)

    With sld.Shapes.Title.TextFrame.TextRange
        .Text = slideTitle
        .Font.Size = 28
        .Font.Bold = True
    End With

    With sld.Shapes.Placeholders(2).TextFrame.TextRange
        .Text = bodyText
        .Font.Size = 14
        .ParagraphFormat.SpaceWithin = 1
    End With

    ' 줄 앞의 [화면]/[데이터]/[링크] 표시에 맞춰 들여쓰기 레벨은 유지, 폰트만 라인별로 강조하고 싶다면
    ' 아래처럼 라인 단위 반복(FindReplace, Split 등)으로 확장 가능합니다. 기본은 단일 스타일로 처리합니다.
End Sub
```

---

## Part1 (슬라이드 1~10) — 표지 / IA / 공통 / 로그인①

```vba
Sub CreateSlides_Part1()

    AddSlide "0.1 표지", _
        "INOUT — B2B 발주·재고 관리 시스템" & vbCrLf & _
        "AS-IS 상세 스토리보드 (코드 기반 자동 분석본)" & vbCrLf & _
        "작성기준일: 2026-07-06" & vbCrLf & _
        "React 19 SPA + Spring Boot 3.5.7 / MySQL / Redis / JWT"

    AddSlide "0.2 INDEX (목차)", _
        "1. Information Architecture" & vbCrLf & _
        "2. 공통(Common) - 레이아웃/내비게이션" & vbCrLf & _
        "3. 로그인 / 회원가입 / 계정찾기" & vbCrLf & _
        "4. 대시보드   5. 재고 관리   6. 발주 관리" & vbCrLf & _
        "7. 배송 관리(NEW)   8. 예치금 관리(NEW)" & vbCrLf & _
        "9. 직원(회원) 관리   10. 문의사항 관리" & vbCrLf & _
        "11. 연차(휴가) 관리(NEW)   12. AI 자동발주(NEW)" & vbCrLf & _
        "13. 원본 대비 변경/미구현 총정리"

    AddSectionSlide "1. Information Architecture", "직원(EMPLOYEE) / 관리자(ADMIN) 역할별 화면 트리 구조"

    AddSlide "1.1 IA - 직원(EMPLOYEE)", _
        "[구조] Home(/emp/dashboard) 하위" & vbCrLf & _
        " - 대시보드 / 재고 조회 / 재고 사용 처리" & vbCrLf & _
        " - 발주 내역(상세→결제) / 배송 현황(NEW)" & vbCrLf & _
        " - 연차 신청(등록/상세)(NEW) / 장바구니" & vbCrLf & _
        " - 문의 사항(작성/상세) / 나의 예치금(NEW) / 내 정보" & vbCrLf & _
        "[구현] React Router v7 기반 SPA, Layout 하위 중첩 라우트"

    AddSlide "1.2 IA - 관리자(ADMIN)", _
        "[구조] Home(/admin/dashboard) 하위" & vbCrLf & _
        " - 대시보드(AI 인사이트 포함) / 발주 관리(상세처리)" & vbCrLf & _
        " - 배송 관리(NEW) / 재고 관리(등록/상세)" & vbCrLf & _
        " - 직원 관리(상세) / 문의 사항(상세)" & vbCrLf & _
        " - 예치금 관리(NEW) / 연차 관리(NEW) / 내 정보(공용)" & vbCrLf & _
        "[구현] SecurityConfig 기준 /api/admin/** = ROLE_ADMIN"

    AddSectionSlide "2. 공통(Common)", "모든 화면에 적용되는 레이아웃과 내비게이션 규칙"

    AddSlide "2.1 사이드바 내비게이션 + 헤더/로그아웃", _
        "[화면] 좌측 고정 사이드바(다크 네이비), 로고 INOUT" & vbCrLf & _
        "[화면] 역할별 메뉴 자동 전환(ADMIN/EMPLOYEE), 하단 사용자 정보+로그아웃" & vbCrLf & _
        "[화면] 상단 Topbar: 페이지 제목, ADMIN 배지, 장바구니 아이콘(직원)" & vbCrLf & _
        "[데이터] JWT accessToken payload를 클라이언트에서 디코딩해 role 판별" & vbCrLf & _
        "[데이터] 로그아웃: POST /api/user/logout" & vbCrLf & _
        "[링크] 메뉴 클릭 시 SPA 라우팅(새로고침 없음), 로그아웃 확인 후 /login 이동" & vbCrLf & _
        "[미구현] 원본의 공통 Footer(공지/약관/개인정보처리방침)는 대응 화면 없음"

    AddSectionSlide "3. 로그인 / 회원가입 / 계정찾기", "인증 관련 화면 일체 (1/2)"

    AddSlide "3.1 일반 로그인", _
        "[화면] INOUT SYSTEM 타이틀, 이메일/비밀번호 입력, 자동 로그인 체크박스" & vbCrLf & _
        "[화면] 로그인 버튼 + 소셜 로그인 3종, 이메일로 가입하기, 아이디/비밀번호 찾기 링크" & vbCrLf & _
        "[데이터] POST /api/user/login {email, password} -> accessToken/refreshToken/role" & vbCrLf & _
        "[데이터] 로그인 5회 연속 실패 시 계정 자동 잠금(403 ACCOUNT_LOCKED)" & vbCrLf & _
        "[링크] 성공 시 role에 ADMIN 포함 -> /admin/dashboard, 아니면 /emp/dashboard" & vbCrLf & _
        "[링크] '이메일로 가입하기' -> /register, '아이디/비밀번호 찾기' -> /find-account"

    AddSlide "3.2 소셜 로그인(OAuth2) (NEW)", _
        "[화면] Google/Kakao/Naver로 시작하기 버튼(LoginPage)" & vbCrLf & _
        "[데이터] GET /oauth2/authorization/{provider} -> 콜백 /login/oauth2/code/{provider}" & vbCrLf & _
        "[데이터] 신규 사용자는 자동 회원가입 + ROLE_EMPLOYEE 부여" & vbCrLf & _
        "[데이터] 성공 시 {FRONTEND_URL}/oauth2/callback?accessToken=...&role=... 로 302 리다이렉트" & vbCrLf & _
        "[링크] 콜백 페이지에서 토큰 저장 후 대시보드로 이동" & vbCrLf & _
        "[구현노트] 백엔드 3사 연동 완료, 단 프론트 카카오 버튼은 '준비중' 안내만 노출"

End Sub
```

---

## Part2 (슬라이드 11~20) — 로그인② / 대시보드 / 재고관리①

```vba
Sub CreateSlides_Part2()

    AddSlide "3.3 회원가입", _
        "[화면] Email/Password/Confirm/Name/Phone/Store(Select)/Birthday 입력" & vbCrLf & _
        "[화면] 이메일 '중복확인' 버튼, '가입하기', '로그인으로 돌아가기'" & vbCrLf & _
        "[데이터] GET /api/user/public/check-email, GET /api/user/public/stores(1시간 캐시)" & vbCrLf & _
        "[데이터] POST /api/user/register -> {redirectUrl}" & vbCrLf & _
        "[링크] 가입 성공 시 안내 후 /login 이동" & vbCrLf & _
        "[미구현] 원본의 이용약관 동의 체크박스는 현재 화면에 없음"

    AddSlide "3.4 아이디 찾기 / 비밀번호 찾기", _
        "[화면] 탭 전환형 단일 페이지 (아이디 찾기 / 비밀번호 찾기)" & vbCrLf & _
        "[화면] 아이디 찾기: Name/Phone 입력, 비밀번호 찾기: Email/Name/Phone 입력" & vbCrLf & _
        "[데이터] POST /api/user/find {name, phone} -> {email 등}" & vbCrLf & _
        "[데이터] POST /api/user/public/password/reset -> resetKey(30분 유효) 메일 발송" & vbCrLf & _
        "[링크] '로그인으로 돌아가기' -> /login" & vbCrLf & _
        "[구현노트] 원본의 별도 결과 페이지 대신 동일 화면 내 인라인 결과로 통합, 마스킹 미적용"

    AddSlide "3.5 비밀번호 재설정", _
        "[화면] 메일 링크(?key=)로 진입, New/Confirm Password 입력, 실시간 일치 안내" & vbCrLf & _
        "[데이터] GET /api/user/public/password/reset/check?key=" & vbCrLf & _
        "[데이터] POST /api/user/public/resetPassword?resetKey=&newPassword=&confirmPassword=" & vbCrLf & _
        "[링크] 변경 성공 시 안내 후 /login 이동"

    AddSectionSlide "4. 대시보드", "직원/관리자 요약 지표 및 AI 인사이트"

    AddSlide "4.1 대시보드 (직원)", _
        "[화면] KPI: 예치금 잔액/장바구니 수/진행중 발주/금일 재고 사용" & vbCrLf & _
        "[화면] 재고 상태 진행바, 발주 진행 현황, 알림 피드, 바로가기 버튼" & vbCrLf & _
        "[데이터] GET /api/emp/dashboard/summary -> DashboardEmpResponse" & vbCrLf & _
        "[링크] 바로가기 클릭 -> /emp/deposit, /emp/cart, /emp/orders, /emp/stocks, /emp/inquiries"

    AddSlide "4.2 대시보드 (관리자)", _
        "[화면] KPI: 금일 신규발주/주문액/배송중/배송완료" & vbCrLf & _
        "[화면] 재고상태 분포, 발주처리 현황, 당일 배송 현황, 실시간 알림, 미읽음 문의 배너" & vbCrLf & _
        "[데이터] GET /api/dashboard/summary (Redis 5분 캐시) -> DashboardSummaryResponse" & vbCrLf & _
        "[링크] 패널 클릭 -> /admin/orders, /admin/delivery, /admin/stocks, /admin/users, Swagger"

    AddSlide "4.3 AI 운영 인사이트 (NEW)", _
        "[화면] 'AI 운영 인사이트 분석' 패널, GEMINI 배지, 'AI 분석 시작/재분석' 버튼" & vbCrLf & _
        "[데이터] GET /api/admin/ai/insight (비동기, 35초 타임아웃) -> report/generatedAt/model" & vbCrLf & _
        "[데이터] 최근 6개월 발주/재고/매장별 통계를 Gemini 2.5-flash로 분석" & vbCrLf & _
        "[링크] 버튼 클릭 시에만 온디맨드 호출, 실패 시 패널 내 인라인 에러 메시지"

    AddSectionSlide "5. 재고 관리", "직원 조회/사용 처리, 관리자 등록/상세/실사 (1/2)"

    AddSlide "5.1 재고 조회 (직원)", _
        "[화면] 상품명 검색, 목록 테이블(상품명/카테고리/단가/재고), 상세 모달 + 담기" & vbCrLf & _
        "[데이터] GET /api/emp/stocks?name=&page=&size= -> Page<ItemResponse>" & vbCrLf & _
        "[데이터] GET /api/emp/stocks/{itemId}, POST /api/emp/carts {itemId, quantity}" & vbCrLf & _
        "[링크] '+담기' -> 모달 -> 장바구니 담기 성공 Toast"

    AddSlide "5.2 재고 사용 처리 (직원)", _
        "[화면] 카테고리/상품명 필터, 수량 입력 + '사용처리' 버튼, 재고 0 뱃지" & vbCrLf & _
        "[데이터] POST /api/emp/stocks/use {itemId, quantity, memo} (ROLE_EMPLOYEE 전용)" & vbCrLf & _
        "[데이터] 비관적 락 조회 -> 재고 차감 -> StockUsageHistory 저장, 부족 시 400 NOT_ENOUGH_STOCK" & vbCrLf & _
        "[링크] '사용처리' -> confirm -> 성공 시 목록 즉시 갱신(새로고침 없음)"

End Sub
```

---

## Part3 (슬라이드 21~30) — 재고관리② / 발주관리

```vba
Sub CreateSlides_Part3()

    AddSlide "5.3 재고 목록 (관리자)", _
        "[화면] 요약 카드(총 활성/정상/저재고/품절), 저재고 배너, 검색, '신규 상품 등록'" & vbCrLf & _
        "[데이터] GET /api/admin/stocks?name=&deleted=&page=&size= -> Page<StockAdminResponse>" & vbCrLf & _
        "[데이터] GET /api/admin/stocks/alerts/low-stock (Redis 캐시)" & vbCrLf & _
        "[링크] 상품명 클릭 -> /admin/stocks/{itemId}, '신규 상품 등록' -> /admin/stocks/new"

    AddSlide "5.4 상품 등록 (관리자)", _
        "[화면] 대표 이미지, 상품명/카테고리/단가*, 안전재고/단위설명/상세설명" & vbCrLf & _
        "[데이터] POST /api/admin/images/upload (multipart) -> 이미지 URL" & vbCrLf & _
        "[데이터] POST /api/admin/stocks {name,categoryId,unitPrice,...} -> 신규 itemId" & vbCrLf & _
        "[링크] 등록/취소 후 /admin/stocks 이동" & vbCrLf & _
        "[구현노트] 반환 이미지 URL 경로(/uploads/..)와 정적 매핑 설정 불일치 가능성 존재"

    AddSlide "5.5 재고 상세 / 통합 이력 / 재고 실사 (관리자)", _
        "[화면] 현재상태/현재재고/안전재고, '재고 실사 입력'(실제수량+사유), 입출고 이력 테이블" & vbCrLf & _
        "[데이터] GET /api/admin/stocks/{itemId}?page=&size= -> StockDetailResponse(history 포함)" & vbCrLf & _
        "[데이터] PATCH /api/admin/stocks/{itemId}/adjust {adjustedQuantity, reason}" & vbCrLf & _
        "[링크] '실사 반영하기' -> 성공 Toast, 뒤로가기 -> /admin/stocks" & vbCrLf & _
        "[미구현] 전용 '입고 처리' 화면, '전체 통합 입고 이력' 화면 (API는 존재, 실사 화면으로 대체 운용)"

    AddSectionSlide "6. 발주 관리", "장바구니 → 신청 → 결제 → 관리자 처리 전체 흐름"

    AddSlide "6.1 장바구니 (직원)", _
        "[화면] 상품별 수량 +/- 조절, 삭제, 총 수량/총 결제 예상 금액, '전체 발주 결제하기'" & vbCrLf & _
        "[데이터] GET /api/emp/carts -> CartResponse{items, totalQuantity, totalPrice}" & vbCrLf & _
        "[데이터] PATCH /api/emp/carts/{cartDetailId}/quantity, DELETE /api/emp/carts/items" & vbCrLf & _
        "[링크] 결제하기 -> 발주 신청 성공 시 /emp/payment/{orderId}"

    AddSlide "6.2 발주 신청 (미리보기 포함) (직원)", _
        "[화면] 장바구니 선택 품목 -> 결제 화면으로 자동 전환(별도 등록 폼 없음)" & vbCrLf & _
        "[데이터] POST /api/emp/orders/preview {cartDetailIds, memo, receiver...} -> 미리보기(DB저장X)" & vbCrLf & _
        "[데이터] POST /api/emp/orders -> orderRequestId 생성(상태=REQUESTED, 결제대기)" & vbCrLf & _
        "[링크] 신청 성공 -> /emp/payment/{orderId}" & vbCrLf & _
        "[구현노트] OrderCreateRequest.memo 필드는 실제 저장 로직에 매핑되지 않는 누락 필드"

    AddSlide "6.3 발주 결제(예치금) (직원) (NEW)", _
        "[화면] 주문 요약, 결제 정보(총 금액/현재 예치금/최종 결제/결제 후 잔액)" & vbCrLf & _
        "[화면] 예치금 부족 시 안내 + 결제 버튼 비활성화" & vbCrLf & _
        "[데이터] POST /api/payment/deposit {orderId, amount} -> 예치금 차감 후 발주 PAID 전이" & vbCrLf & _
        "[링크] '결제하기' -> confirm -> 성공 시 /emp/orders 이동"

    AddSlide "6.4 발주 내역 목록/상세 (직원)", _
        "[화면] 목록: 주문번호/신청일시/대표상품/금액/상태(7종 뱃지)" & vbCrLf & _
        "[화면] 상세: 배송정보/품목 테이블, '발주 취소'/'결제하기'" & vbCrLf & _
        "[데이터] GET /api/emp/orders, GET /api/emp/orders/{orderId}" & vbCrLf & _
        "[데이터] PATCH /api/emp/orders/{orderId}/cancel (PAID는 예치금 전액 환불 후 취소)" & vbCrLf & _
        "[링크] 목록 클릭 -> /emp/orders/{id}, 결제하기 -> /emp/payment/{orderId}"

    AddSlide "6.5 발주 관리 목록 (관리자)", _
        "[화면] 요약 카드, 탭(전체/승인대기/부분처리/완료/취소반려), 엑셀 다운로드" & vbCrLf & _
        "[화면] 체크박스 일괄 선택 -> '선택 발주 일괄 승인', 결과 모달" & vbCrLf & _
        "[데이터] GET /api/admin/orders?status=, POST /api/admin/orders/bulk-approve" & vbCrLf & _
        "[데이터] GET /api/admin/orders/excel (Apache POI, xlsx 스트림)" & vbCrLf & _
        "[링크] '상세보기' -> /admin/orders/{id}"

    AddSlide "6.6 발주 상세 처리 (관리자)", _
        "[화면] 신청 정보, '대기 품목 전체 승인', 품목별 승인/반려/대기 버튼" & vbCrLf & _
        "[데이터] GET /api/admin/orders/{orderId}, PATCH /api/admin/orders/{orderId}/process" & vbCrLf & _
        "[데이터] 전체승인->COMPLETED+배송자동생성, 전체반려->REJECTED, 혼합->PARTIAL" & vbCrLf & _
        "[링크] 버튼 클릭 시 즉시 비동기 처리, 목록 재조회 없이 상태만 갱신" & vbCrLf & _
        "[구현노트] 품목 상태는 WAITING/APPROVED/DELAYED/REJECTED 4종으로 원본 대비 확장"

End Sub
```

---

## Part4 (슬라이드 31~40) — 배송 / 예치금 / 직원관리①

```vba
Sub CreateSlides_Part4()

    AddSectionSlide "7. 배송 관리 (NEW)", "발주 완료 후 자동 생성되는 배송 프로세스"

    AddSlide "7.1 배송 현황 (직원)", _
        "[화면] 탭(전체/준비중/배송중/완료), 목록 클릭 시 발주+배송 상세 모달" & vbCrLf & _
        "[데이터] GET /api/emp/deliveries?status=&page=&size= -> Page<DeliveryDto.ListItem>(본인 건만)" & vbCrLf & _
        "[링크] 행 클릭 -> 모달에서 GET /api/emp/orders/{orderId} 조회 표시"

    AddSlide "7.2 배송 관리 (관리자)", _
        "[화면] 탭(전체/준비/배송중/완료), '발송 처리'/'배송 완료' 액션 버튼" & vbCrLf & _
        "[데이터] GET /api/admin/deliveries?status=, PATCH .../start, PATCH .../complete" & vbCrLf & _
        "[데이터] 발주 전품목 승인(COMPLETED) 시 OrderApprovedEvent -> 배송(READY) 자동 생성" & vbCrLf & _
        "[링크] 버튼 클릭 -> confirm -> 비동기 처리 후 목록 즉시 갱신"

    AddSectionSlide "8. 예치금 관리 (NEW)", "직원 잔액 조회/충전, 관리자 통합 관리"

    AddSlide "8.1 나의 예치금 (직원)", _
        "[화면] 사용 가능 잔액, '충전하기' 모달(빠른금액 버튼), 거래 내역 테이블" & vbCrLf & _
        "[데이터] GET /api/emp/deposit?page=&size= -> {currentBalance, histories}" & vbCrLf & _
        "[데이터] POST /api/deposit/charge {amount, description} (즉시 충전)" & vbCrLf & _
        "[링크] 충전 성공 시 잔액/내역 즉시 갱신"

    AddSlide "8.2 예치금 충전 요청 / 승인·반려 (NEW)", _
        "[미구현] 전용 프론트 화면 없음 - 아래는 구현된 백엔드 API 명세" & vbCrLf & _
        "[데이터] POST /api/emp/charges {amount} -> ChargeRequest(PENDING)" & vbCrLf & _
        "[데이터] GET /api/emp/charges, GET /api/admin/charges/pending" & vbCrLf & _
        "[데이터] PATCH /api/admin/charges/{id}/approve, PATCH .../reject {reason}" & vbCrLf & _
        "[구현노트] 실제 서비스는 8.1의 즉시충전 API를 사용 중 (승인 플로우는 미연동)"

    AddSlide "8.3 예치금 관리 (관리자)", _
        "[화면] 요약(총 잔액/이달 충전/이달 사용), 필터, '가맹점 수동 지급' 모달" & vbCrLf & _
        "[데이터] GET /api/admin/deposits?storeId=&type=&keyword= -> {summary, histories}" & vbCrLf & _
        "[데이터] GET /api/admin/deposits/franchisees, POST /api/admin/deposits/charge" & vbCrLf & _
        "[링크] 수동 지급 모달 확인 -> 목록 즉시 갱신"

    AddSectionSlide "9. 직원(회원) 관리", "관리자의 직원 계정 관리 화면 (1/2)"

    AddSlide "9.1 직원 목록 (관리자)", _
        "[화면] 요약(총직원/재직/휴직/잠긴계정), 매장/상태 필터, 검색" & vbCrLf & _
        "[화면] 테이블: 회원정보/소속매장/상태/계정잠금/가입일" & vbCrLf & _
        "[데이터] GET /api/admin/users?storeId=&status=&keyword=&page=&size=" & vbCrLf & _
        "[링크] 행 클릭 -> /admin/users/{id} (상세 데이터는 state로 전달, 별도 상세 API 없음)"

    AddSlide "9.2 직원 상세 (관리자)", _
        "[화면] 재직상태/계정잠금 요약, 소속매장/재직상태/ADMIN 권한 체크박스" & vbCrLf & _
        "[화면] '계정 잠금 해제', '비밀번호 초기화 메일 발송', '변경사항 저장'" & vbCrLf & _
        "[데이터] PUT /api/admin/users/{id}, PATCH .../unlock, POST .../reset-password" & vbCrLf & _
        "[링크] 저장 성공 -> /admin/users 복귀"

End Sub
```

---

## Part5 (슬라이드 41~50) — 직원관리② / 문의사항 / 연차관리 / AI자동발주

```vba
Sub CreateSlides_Part5()

    AddSlide "9.3 내 정보 조회/수정 (공통)", _
        "[화면] 이메일/이름/매장명/생년월일/연락처/상태 조회, 수정 폼, 비밀번호 변경 모달" & vbCrLf & _
        "[데이터] GET /api/user/profile, PUT /api/user/profile {name, storeName, phone, password}" & vbCrLf & _
        "[데이터] PATCH /api/user/profile/password {password, newPassword}" & vbCrLf & _
        "[링크] '정보 수정하기' -> 인라인 폼 -> '수정 완료'"

    AddSectionSlide "10. 문의사항 관리", "직원 문의 작성 및 관리자/직원 간 댓글 소통"

    AddSlide "10.1 문의사항 목록 (직원/관리자)", _
        "[화면] 직원: '+문의 작성하기' + 테이블, 관리자: 전체 목록(작성 버튼 없음)" & vbCrLf & _
        "[데이터] GET /api/inquiry?page=&size= (ADMIN=전체, EMPLOYEE=본인 글만)" & vbCrLf & _
        "[링크] 작성 -> /emp/inquiries/new, 행 클릭 -> 상세 페이지" & vbCrLf & _
        "[구현노트] Swagger 설명과 달리 매장 공유가 아닌 작성자 개인 단위로 구현됨"

    AddSlide "10.2 문의사항 작성 (직원)", _
        "[화면] 제목*/내용*/첨부파일(선택, 10MB 이하), '취소'/'문의 등록하기'" & vbCrLf & _
        "[데이터] POST /api/inquiry (multipart) {title, content, file}" & vbCrLf & _
        "[링크] 등록 성공 -> /emp/inquiries"

    AddSlide "10.3 문의사항 상세 및 댓글/답글", _
        "[화면] 제목/내용/첨부 다운로드, 댓글 목록, '답글/수정/삭제' (2단계까지만 허용)" & vbCrLf & _
        "[데이터] GET /api/inquiry/{id} (ADMIN 열람 시 자동 읽음처리)" & vbCrLf & _
        "[데이터] POST/PUT/DELETE /api/inquiry/{id}/comments/{commentId}" & vbCrLf & _
        "[링크] '글 삭제'는 작성자 본인만 가능(관리자도 타인 글 삭제 불가)"

    AddSectionSlide "11. 연차(휴가) 관리 (NEW)", "직원 연차 신청/조회, 관리자 심사 처리"

    AddSlide "11.1 연차 신청 (직원)", _
        "[화면] 시작일/종료일, 연차 종류(연차/반차/병가), 사유 Textarea" & vbCrLf & _
        "[화면] 시작일>종료일 시 브라우저 alert로 1차 방어, 성공 시 완료 팝업" & vbCrLf & _
        "[데이터] POST /api/emp/vacation {startDate, endDate, type, reason}" & vbCrLf & _
        "[데이터] 기간 겹침/역전 시 InoutException 발생, 신규 신청은 PENDING" & vbCrLf & _
        "[링크] 제출 성공 -> /emp/vacation 목록 이동"

    AddSlide "11.2 연차 신청 목록/상세 (직원)", _
        "[화면] 신청일자/연차기간/종류/상태(대기/승인/반려/보류) 뱃지, 페이징" & vbCrLf & _
        "[화면] 반려 상태 클릭 시 관리자 반려 사유 명확히 표시" & vbCrLf & _
        "[데이터] GET /api/emp/vacation?page=&size=, GET /api/emp/vacation/{leaveId}" & vbCrLf & _
        "[링크] 목록 클릭 -> 상세, '연차 신청' -> /emp/vacation/new"

    AddSlide "11.3 연차 심사 및 처리 (관리자)", _
        "[화면] 상태별 탭, 목록 각 행 우측 '승인/반려/보류' 버튼" & vbCrLf & _
        "[화면] '반려' 클릭 시 사유 입력 모달 활성화 후 최종 처리" & vbCrLf & _
        "[데이터] GET /api/admin/vacation?status=, PATCH /api/admin/vacation/{leaveId} {status, rejectReason}" & vbCrLf & _
        "[데이터] REJECTED인데 rejectReason 없으면 InoutException 발생" & vbCrLf & _
        "[링크] 승인/보류/반려 모두 비동기 처리, 새로고침 없이 목록 즉시 갱신"

    AddSectionSlide "12. AI 자동발주 (NEW)", "저재고 품목에 대한 Gemini 기반 자동 발주 초안 생성"

End Sub
```

---

## Part6 (슬라이드 51~56) — AI자동발주 / 원본대비 총정리 / 마무리

```vba
Sub CreateSlides_Part6()

    AddSlide "12.1 AI 자동발주 스케줄러 / 수동 트리거", _
        "[미구현] 전용 프론트 화면 없음 (대시보드 AI 인사이트와는 별개 기능)" & vbCrLf & _
        "[데이터] @Scheduled(cron 매일 자정) AiAutoOrderService.createAutoOrderDraft()" & vbCrLf & _
        "[데이터] POST /api/admin/ai/auto-order (수동 트리거) -> 통합 발주서 1건 자동 생성(REQUESTED)" & vbCrLf & _
        "[링크] 생성된 초안은 /admin/orders 승인 대기 건으로 노출되어 6장 화면에서 동일 처리"

    AddSectionSlide "13. 원본 대비 변경/미구현 총정리", "코드 기반 재작성에 따른 차이점 요약"

    AddSlide "13.1 신규 추가 기능 (NEW)", _
        "- 3.2 소셜 로그인(Google/Kakao/Naver)" & vbCrLf & _
        "- 4.3 AI 운영 인사이트(Gemini)" & vbCrLf & _
        "- 6.3 예치금 결제 방식 신규 도입" & vbCrLf & _
        "- 6.5 발주 일괄 승인 + 엑셀 다운로드" & vbCrLf & _
        "- 7장 배송 관리 전체 / 8장 예치금 관리 전체" & vbCrLf & _
        "- 11장 연차(휴가) 관리 전체 / 12장 AI 자동발주"

    AddSlide "13.2 원본에는 있으나 현재 미구현", _
        "- 2.1 공통 Footer(공지/약관/개인정보처리방침)" & vbCrLf & _
        "- 3.4 회원가입 이용약관 동의 체크박스" & vbCrLf & _
        "- 5.8 재고 입고 처리 전용 화면 (API는 존재)" & vbCrLf & _
        "- 5.9 전체 입고 이력 통합 조회 화면" & vbCrLf & _
        "- 5.6 상품 사진 개별 수정/삭제 API" & vbCrLf & _
        "- 8.2 충전 요청→승인 전용 프론트 화면"

    AddSlide "13.3 원본과 달라진 정책 (구현 노트)", _
        "- 발주 품목 상태: 3종 -> WAITING/APPROVED/DELAYED/REJECTED 4종으로 확장" & vbCrLf & _
        "- 발주 헤더 상태: REQUESTED->PAID->(PARTIAL|COMPLETED|REJECTED)->CANCELLED (예치금 결제 단계 추가)" & vbCrLf & _
        "- 문의사항: 매장 단위 공유가 아닌 작성자 개인 단위 조회/삭제 권한" & vbCrLf & _
        "- 아이디 찾기: 별도 페이지가 아닌 동일 화면 내 인라인 결과, 마스킹 미적용"

    AddSlide "부록. 문서 정보", _
        "본 스토리보드는 실제 Controller/DTO/Entity/Service 코드를 전수 분석하여 자동 생성되었습니다." & vbCrLf & _
        "원본 파일: docs/storyboard/AS-IS_스토리보드.md" & vbCrLf & _
        "작성 기준일: 2026-07-06"

    AddSlide "감사합니다", _
        "INOUT — B2B 발주·재고 관리 시스템" & vbCrLf & _
        "AS-IS 상세 스토리보드 (End of Document)"

End Sub
```

---

## 참고 사항

- `AddSlide`/`AddSectionSlide` 헬퍼가 매 실행 시 현재 프레젠테이션 **맨 뒤에** 슬라이드를 추가합니다. 재실행 시 중복 생성되니, 다시 만들고 싶다면 기존 슬라이드를 모두 지우고 실행하세요.
- 슬라이드 디자인(테마/색상/폰트)은 매크로가 건드리지 않으므로, 생성 후 PowerPoint의 `디자인` 탭에서 원하는 테마를 적용하면 됩니다.
- 본문 텍스트가 많은 슬라이드(6.6, 13.1 등)는 생성 후 자동 글자 크기 조정이 될 수도 있고 넘칠 수도 있습니다. 필요 시 `AddSlide`의 `.Font.Size = 14` 값을 12로 낮추거나, 슬라이드를 2개로 나눠서 호출하세요.
- 특정 파트만 다시 실행하고 싶다면 `CreateSlides_PartN`을 개별적으로 호출하면 됩니다.
