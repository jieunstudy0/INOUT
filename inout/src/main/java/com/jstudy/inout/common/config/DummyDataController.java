package com.jstudy.inout.common.config;

import com.jstudy.inout.common.dto.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "더미 데이터 관리", description = "테스트 환경 데이터 초기화 (로컬/데모 환경 전용)")
@RestController
@RequestMapping("/api/dummy")
@Profile({"local", "demo", "dev"})
@RequiredArgsConstructor
public class DummyDataController {

    private final DummyDataService dummyDataService;

    @Operation(
            summary = "더미 데이터 전체 초기화 및 재생성",
            description = """
                    기존 DB 데이터(상품·매장·유저 포함)를 모두 삭제한 뒤, 테스트용 초기 데이터를 다시 생성합니다.
                    - ROLE_ADMIN: admin1@test.com, admin2@test.com (store=null)
                    - ROLE_OWNER: owner1~5@test.com (지점 1~5호)
                    - ROLE_EMPLOYEE: emp1~20@test.com (지점 1~5호 분산)
                    - 비밀번호: inout1234!
                    """)
    @PostMapping("/reset")
    public ResponseEntity<?> resetDummyData() {
        dummyDataService.clearAllData();
        dummyDataService.generateDummyData();
        return ResponseResult.successWithMessage("더미 데이터가 성공적으로 초기화 및 생성되었습니다.");
    }

    @Operation(
            summary = "유저·권한만 3역할로 재초기화 (상품·매장 유지)",
            description = """
                    상품(Item), 카테고리(ItemCategory), 매장(Store) 데이터는 삭제하지 않고 보존합니다.
                    유저 및 유저 연관 데이터(연차·문의·주문·배송·장바구니·예치금·재고이력·UserRole 등)만
                    FK 순서에 맞춰 삭제한 뒤, 아래 3역할 시드 계정을 다시 생성합니다.
                    - ROLE_ADMIN: admin1@test.com, admin2@test.com (store=null)
                    - ROLE_OWNER: owner1~5@test.com (지점 1~5호) + 매장 예치금 5천만원
                    - ROLE_EMPLOYEE: emp1~20@test.com (지점 1~5호 분산)
                    - 비밀번호: inout1234!
                    """)
    @PostMapping("/reset-users")
    public ResponseEntity<?> resetUsersOnly() {
        dummyDataService.resetUsersOnly();
        return ResponseResult.successWithMessage(
                "상품·매장은 유지한 채 유저·권한이 3역할 구조로 재초기화되었습니다.");
    }
}
