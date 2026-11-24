package com.jstudy.inout.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.count.StockCountLineResponse;
import com.jstudy.inout.stock.entity.StockCountSheet.AiReading;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** AI 응답을 그대로 믿지 않고, 받아들일 값만 남기는지 확인한다. */
class AiStockCountReaderTest {

    private final AiStockCountReader reader = new AiStockCountReader(new ObjectMapper());

    @Test
    @DisplayName("목록에 없는 줄 번호, 음수·소수·문자 수량은 버리거나 '읽지 못함'(null)으로 바꾼다")
    void parse_filtersInvalidValues() {
        String json = """
                {"sheetCode": "SC251123-0900", "lines": [
                  {"lineNo": 1, "countedQty": 3, "confident": true},
                  {"lineNo": 2, "countedQty": -1, "confident": true},
                  {"lineNo": 3, "countedQty": 2.5, "confident": true},
                  {"lineNo": 4, "countedQty": "7", "confident": true},
                  {"lineNo": 5, "countedQty": 9, "confident": false},
                  {"lineNo": 99, "countedQty": 1, "confident": true}
                ]}
                """;

        AiStockCountReader.Result result = reader.parse(json, Set.of(1, 2, 3, 4, 5));

        assertThat(result.sheetCode()).isEqualTo("SC251123-0900");
        assertThat(result.readings()).doesNotContainKey(99);
        assertThat(result.readings().get(1)).isEqualTo(new AiReading(3, true));
        assertThat(result.readings().get(2)).isEqualTo(new AiReading(null, false));
        assertThat(result.readings().get(3)).isEqualTo(new AiReading(null, false));
        assertThat(result.readings().get(4)).isEqualTo(new AiReading(null, false));
        assertThat(result.readings().get(5)).isEqualTo(new AiReading(9, false));
    }

    @Test
    @DisplayName("코드블록으로 감싼 응답도 읽고, sheetCode가 \"null\" 문자열이면 못 읽은 것으로 본다")
    void parse_codeFenceAndNullCode() {
        String text = "```json\n{\"sheetCode\": \"null\", \"lines\": [{\"lineNo\": 1, \"countedQty\": 0, \"confident\": true}]}\n```";

        AiStockCountReader.Result result = reader.parse(text, Set.of(1));

        assertThat(result.sheetCode()).isNull();
        assertThat(result.readings().get(1)).isEqualTo(new AiReading(0, true));
    }

    @Test
    @DisplayName("JSON이 아니면 AI_PARSE_ERROR")
    void parse_notJson() {
        assertThatThrownBy(() -> reader.parse("죄송합니다, 읽을 수 없습니다.", Set.of(1)))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.AI_PARSE_ERROR);
    }

    @Test
    @DisplayName("프롬프트에는 시스템 수량을 넣지 않는다 (AI가 손글씨 대신 인쇄된 값을 베끼지 않게)")
    void prompt_excludesSystemQty() {
        StockCountLineResponse line = StockCountLineResponse.builder()
                .lineNo(1).itemName("원두 1kg").location("C-01").systemQty(12345).build();

        String prompt = reader.buildPrompt("SC251123-0900", List.of(line));

        assertThat(prompt).contains("1. 원두 1kg (위치 C-01)").doesNotContain("12345");
    }

    @Test
    @DisplayName("API 키가 없으면 Gemini를 부르지 않고 AI_NOT_CONFIGURED")
    void read_withoutApiKey() {
        ReflectionTestUtils.setField(reader, "apiKey", "");

        assertThatThrownBy(() -> reader.read(new byte[] {1}, "image/jpeg", "SC1", List.of()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.AI_NOT_CONFIGURED);
    }
}
