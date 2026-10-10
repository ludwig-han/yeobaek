package com.yeobaek.plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResearchPresentationTest {
    @Test void legacyParagraphKeepsItsAttributionAndSummaryIsBounded() {
        ObjectNode report=new ObjectMapper().createObjectNode();
        for(String section:ResearchPresentation.SECTIONS)report.putArray(section);
        for(int i=0;i<4;i++) {
            ObjectNode item=((ArrayNode)report.get("anchors")).addObject().put("subject","목표"+i).put("detail","위치와 주차 안내.").put("anchorIndex",i).put("state","SOURCED");
            item.putArray("sourceIds").add(0).add(1);item.putArray("evidenceIds").add(0);
        }
        ResearchPresentation.prepare(report);
        assertThat(report.path("summary")).hasSize(3);
        assertThat(report.path("anchors").get(0).path("claims")).hasSize(1);
        assertThat(report.path("summary").get(0).path("claims").get(0).path("sourceIds").toString()).isEqualTo("[0,1]");
    }
}
