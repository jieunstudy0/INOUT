package com.jstudy.inout.stock.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.ai.service.AiStockCountReader;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.util.FileStorageUtil;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실사 영수증 사진을 받아 AI로 실재고를 읽고, 읽은 값을 실사표에 참고용으로 저장한다.
 *
 * 이 클래스에는 일부러 @Transactional을 붙이지 않는다.
 * AI 호출은 수십 초가 걸릴 수 있어서, 그동안 DB 연결과 실사표 락을 잡고 있지 않도록
 * [조회 트랜잭션] → [AI 호출(트랜잭션 밖)] → [저장 트랜잭션]으로 나눈다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockCountScanService {

    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final Set<String> PHOTO_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> MIME_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    private final StockCountService stockCountService;
    private final AiStockCountReader aiStockCountReader;
    private final ImageService imageService;

    public StockCountSheetResponse scan(Long userId, Long sheetId, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new InoutException(ErrorCode.FILE_EMPTY);
        }
        if (photo.getSize() > MAX_PHOTO_BYTES) {
            throw new InoutException(ErrorCode.FILE_TOO_LARGE);
        }
        String extension = FileStorageUtil.extractAllowedExtension(photo.getOriginalFilename(), PHOTO_EXTENSIONS)
                .toLowerCase(Locale.ROOT);

        // 1) 조회: 소속 매장의 작성 중인 실사표인지 확인하고 줄 목록을 가져온다.
        StockCountSheetResponse sheet = stockCountService.getSheetForScan(userId, sheetId);

        // 2) AI 호출 (트랜잭션 밖)
        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            log.error("[재고 실사] 사진 읽기 실패", e);
            throw new InoutException(ErrorCode.FILE_UPLOAD_ERROR, "사진을 읽지 못했습니다. 다시 올려 주세요.");
        }
        AiStockCountReader.Result result = aiStockCountReader.read(
                bytes, MIME_BY_EXTENSION.get(extension), sheet.getSheetCode(), sheet.getLines());

        // 3) 저장: 사진 파일과 읽은 값을 기록한다. 그사이 제출됐으면 상태 검사에서 막힌다.
        String photoUrl = imageService.uploadImage(photo);
        Boolean matched = result.sheetCode() == null ? null
                : normalize(result.sheetCode()).equals(normalize(sheet.getSheetCode()));
        stockCountService.recordAiReading(sheetId, result.readings(), matched, photoUrl);

        log.info("[재고 실사] AI 읽기 완료: id={}, 읽은 줄 {}/{}, 번호 일치={}",
                sheetId, result.readings().values().stream().filter(r -> r.qty() != null).count(),
                sheet.getLines().size(), matched);
        return stockCountService.getSheet(userId, sheetId);
    }

    /** 사진에서 읽은 번호는 공백·하이픈이 빠지거나 섞일 수 있어 글자와 숫자만 비교한다. */
    static String normalize(String code) {
        return code.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
