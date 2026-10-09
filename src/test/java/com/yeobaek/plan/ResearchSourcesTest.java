package com.yeobaek.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResearchSourcesTest {
    final ObjectMapper mapper=new ObjectMapper();
    final Instant checked=Instant.parse("2026-10-09T00:00:00Z");
    @Test void showsOnlyExplicitSourceLinkedMetadataAndMarksOldReviews() {
        ArrayNode sources=mapper.createArrayNode();sources.addObject().put("url","https://blog.naver.com/test/123");
        ArrayNode evidence=mapper.createArrayNode();
        evidence.addObject().put("text","방문 후기. 게시일: 2025-01-01. 식사를 제공받았습니다. 계단이 길어요.").putArray("sourceIds").add(0);
        ArrayNode metadata=mapper.createArrayNode();metadata.addObject().put("sourceId",0)
            .put("publishedDate","2025-01-01").put("dateEvidenceId",0).put("dateQuote","게시일: 2025-01-01")
            .put("promotionEvidenceId",0).put("promotionQuote","식사를 제공받았습니다.");
        ResearchSources.enrich(sources,evidence,metadata,checked);
        assertThat(sources.get(0).path("sourceType").asText()).isEqualTo("네이버 블로그");
        assertThat(sources.get(0).path("old").asBoolean()).isTrue();
        assertThat(sources.get(0).path("promotionBasis").asText()).isEqualTo("식사를 제공받았습니다.");
        assertThat(sources.get(0).path("checkedAt").asText()).isEqualTo(checked.toString());
    }
    @Test void rejectsInventedDatesAdvertisementsAndAmbiguousAttribution() {
        ArrayNode sources=mapper.createArrayNode();sources.addObject().put("url","https://example.org");sources.addObject().put("url","https://example.com");
        ArrayNode evidence=mapper.createArrayNode();
        evidence.addObject().put("text","행사일: 2026-10-08. 협찬 없음.").putArray("sourceIds").add(0);
        evidence.addObject().put("text","공식 안내. 게시일: 2025-01-01").putArray("sourceIds").add(0).add(1);
        ArrayNode metadata=mapper.createArrayNode();
        metadata.addObject().put("sourceId",0).put("publishedDate","2026-10-08").put("dateEvidenceId",0).put("dateQuote","행사일: 2026-10-08")
            .put("promotionEvidenceId",0).put("promotionQuote","협찬 없음.");
        metadata.addObject().put("sourceId",1).put("publishedDate","2025-01-01").put("dateEvidenceId",1).put("dateQuote","게시일: 2025-01-01")
            .put("type","OFFICIAL").put("typeEvidenceId",1).put("typeQuote","공식 안내.");
        metadata.addObject().put("sourceId",0).put("publishedDate","2026-10-01").put("dateEvidenceId",0).put("dateQuote","게시일: 2026-10-01");
        ResearchSources.enrich(sources,evidence,metadata,checked);
        for(var source:sources) {
            assertThat(source.path("publishedDate").asText()).isEmpty();
            assertThat(source.path("promotionBasis").asText()).isEmpty();
            assertThat(source.path("sourceType").asText()).isEqualTo("유형 미확인");
        }
    }
}
