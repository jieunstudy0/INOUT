package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.ai.service.AiStockCountReader;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import com.jstudy.inout.stock.entity.StockCountSheet.AiReading;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class StockCountScanServiceTest {

    @Mock private StockCountService stockCountService;
    @Mock private AiStockCountReader aiStockCountReader;
    @Mock private ImageService imageService;
    @InjectMocks private StockCountScanService scanService;

    private final MockMultipartFile photo =
            new MockMultipartFile("photo", "receipt.jpg", "image/jpeg", new byte[] {1, 2, 3});

    private StockCountSheetResponse openSheet() {
        return StockCountSheetResponse.builder().sheetId(1L).sheetCode("SC251123-0900").lines(List.of()).build();
    }

    @Test
    @DisplayName("AI가 실패하면 사진도 저장하지 않고 실사표도 바꾸지 않는다 (직원은 직접 입력해 제출할 수 있다)")
    void aiFailure_nothingSaved() {
        given(stockCountService.getSheetForScan(7L, 1L)).willReturn(openSheet());
        given(aiStockCountReader.read(any(), anyString(), anyString(), any()))
                .willThrow(new InoutException(ErrorCode.AI_API_ERROR));

        assertThatThrownBy(() -> scanService.scan(7L, 1L, photo))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.AI_API_ERROR);

        verify(imageService, never()).uploadImage(any());
        verify(stockCountService, never()).recordAiReading(any(), anyMap(), any(), any());
    }

    @Test
    @DisplayName("사진에서 읽은 번호는 공백·하이픈을 무시하고 비교한다")
    void sheetCodeMatch_ignoresSeparators() {
        given(stockCountService.getSheetForScan(7L, 1L)).willReturn(openSheet());
        given(aiStockCountReader.read(any(), eq("image/jpeg"), anyString(), any()))
                .willReturn(new AiStockCountReader.Result("sc 251123 0900", Map.of(1, new AiReading(3, true))));
        given(imageService.uploadImage(any())).willReturn("/uploads/x.jpg");

        scanService.scan(7L, 1L, photo);

        verify(stockCountService).recordAiReading(eq(1L), anyMap(), eq(true), eq("/uploads/x.jpg"));
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 AI를 부르기 전에 막는다")
    void nonImage_rejectedBeforeAi() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "receipt.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> scanService.scan(7L, 1L, pdf)).isInstanceOf(InoutException.class);

        verify(aiStockCountReader, never()).read(any(), any(), any(), any());
    }

    @Test
    void normalize() {
        assertThat(StockCountScanService.normalize("sc251123-0900")).isEqualTo("SC2511230900");
    }
}
