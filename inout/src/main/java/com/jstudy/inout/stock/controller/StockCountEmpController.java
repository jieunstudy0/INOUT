package com.jstudy.inout.stock.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import com.jstudy.inout.stock.dto.count.StockCountSubmitRequest;
import com.jstudy.inout.stock.dto.count.StockCountSummaryResponse;
import com.jstudy.inout.stock.service.StockCountScanService;
import com.jstudy.inout.stock.service.StockCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "매장 재고 실사 (직원)", description = "소속 매장 실사 영수증 출력, 사진으로 실재고 읽기, 실재고 제출")
@RestController
@RequestMapping("/api/emp/stock-counts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class StockCountEmpController {

    private final StockCountService stockCountService;
    private final StockCountScanService stockCountScanService;

    @Operation(summary = "최근 실사표 목록", description = "소속 매장의 최근 실사표 30장을 최신순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<?> getRecentSheets(@AuthenticationPrincipal CustomUserDetails principal) {
        List<StockCountSummaryResponse> sheets = stockCountService.getRecentSheets(principal.getUser().getId(), null);
        return ResponseResult.success("실사표 목록 조회가 완료되었습니다.", sheets);
    }

    @Operation(summary = "실사표 지금 만들기",
            description = "소속 매장의 저재고 상품과 최근 24시간 사용 상품으로 실사표를 만듭니다. 매일 9시에는 자동으로 만들어집니다.")
    @PostMapping
    public ResponseEntity<?> createSheet(@AuthenticationPrincipal CustomUserDetails principal) {
        Long sheetId = stockCountService.createSheet(principal.getUser().getId());
        return ResponseResult.success("실사표가 생성되었습니다.", sheetId);
    }

    @Operation(summary = "실사표 상세 (영수증 출력용)")
    @GetMapping("/{sheetId}")
    public ResponseEntity<?> getSheet(@PathVariable("sheetId") Long sheetId,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        StockCountSheetResponse sheet = stockCountService.getSheet(principal.getUser().getId(), sheetId);
        return ResponseResult.success("실사표 조회가 완료되었습니다.", sheet);
    }

    @Operation(summary = "영수증 사진으로 실재고 읽기",
            description = "펜으로 실재고를 적은 영수증 사진(jpg·png·webp, 10MB 이하)을 올리면 AI가 숫자를 읽어 채웁니다. "
                    + "읽은 값은 참고용이며, 확인 후 제출해야 확정됩니다.")
    @PostMapping(value = "/{sheetId}/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scan(@PathVariable("sheetId") Long sheetId,
                                  @RequestPart("photo") MultipartFile photo,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        StockCountSheetResponse sheet = stockCountScanService.scan(principal.getUser().getId(), sheetId, photo);
        return ResponseResult.success("사진 읽기가 완료되었습니다.", sheet);
    }

    @Operation(summary = "실재고 제출", description = "확인한 실재고를 제출합니다. 점주가 승인하면 매장 재고에 반영됩니다.")
    @PostMapping("/{sheetId}/submit")
    public ResponseEntity<?> submit(@PathVariable("sheetId") Long sheetId,
                                    @RequestBody @Valid StockCountSubmitRequest request,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        stockCountService.submit(sheetId, principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("실재고가 제출되었습니다.");
    }
}
