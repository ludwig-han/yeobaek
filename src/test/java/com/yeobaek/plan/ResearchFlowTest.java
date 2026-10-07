package com.yeobaek.plan;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:research;DB_CLOSE_DELAY=-1","research.daily-limit=20"})
@AutoConfigureMockMvc
class ResearchFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ResearchService research;
    @Autowired PlanService plans;
    @Autowired ResearchRunRepository runs;
    @MockitoBean GeminiResearchClient client;
    @BeforeEach void setup(){when(client.available()).thenReturn(true);when(client.model()).thenReturn("gemini-3.8-flash");}
    String create(MockHttpSession owner) throws Exception {
        return mvc.perform(post("/plans").session(owner).with(csrf()).param("title","조사 테스트").param("date","2026-10-08").param("anchor1","화성행궁")
            .param("participants[0].label","비공개 이름").param("participants[0].origin","비공개 출발역").param("transport","기존 비공개 메모"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }
    @Test void authorizesResearchStoresResultMarksStaleAndNeverLeaksOnShare() throws Exception {
        ObjectNode report=mapper.createObjectNode();
        for(String name:new String[]{"anchors","risks","backups","priorities","unknown","flexible","sources","evidence","queries"})report.putArray(name);
        ObjectNode item=((ArrayNode)report.get("risks")).addObject();item.put("subject","출발 검토").put("detail","비공개 출발역 관련 조사").put("state","INFERENCE");item.putArray("sourceIds");item.putArray("evidenceIds");
        report.put("searchSuggestions","");when(client.research(any())).thenReturn(report);
        ((ArrayNode)report.get("sources")).addObject().put("url","https://www.swcf.or.kr/?p=65").put("title","공식 출처");
        ObjectNode evidence=((ArrayNode)report.get("evidence")).addObject().put("text","공식 안내 근거");evidence.putArray("sourceIds").add(0);
        ObjectNode anchor=((ArrayNode)report.get("anchors")).addObject().put("subject","화성행궁").put("detail","<script>alert(1)</script> 근거 확인").put("state","SOURCED");
        anchor.putArray("sourceIds").add(0);anchor.putArray("evidenceIds").add(0);
        MockHttpSession owner=new MockHttpSession();String path=create(owner),id=path.substring(3);
        mvc.perform(post(path+"/research").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path+"/research").session(owner)).andExpect(status().isForbidden());
        mvc.perform(post(path+"/research").session(owner).with(csrf())).andExpect(status().is3xxRedirection());
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(research.latest(id).orElseThrow().isRunning() && System.nanoTime()<deadline)Thread.sleep(20);
        ResearchRun run=research.latest(id).orElseThrow();assertThat(run.isSucceeded()).isTrue();
        ResearchRun persisted=runs.findById(run.getId()).orElseThrow();
        assertThat(mapper.readTree(persisted.getResultJson())).isEqualTo(report);
        assertThat(persisted.getResearchedAt()).isNotNull();
        assertThat(persisted.isStale(plans.get(id))).isFalse();
        mvc.perform(get(path).session(owner)).andExpect(status().isOk()).andExpect(content().string(containsString("비공개 출발역 관련 조사")))
            .andExpect(content().string(containsString("https://www.swcf.or.kr/?p=65")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get(path+"/share").session(owner)).andExpect(status().isOk()).andExpect(content().string(not(containsString("비공개 출발역"))));
        mvc.perform(get(path)).andExpect(content().string(not(containsString("비공개 출발역"))));
        mvc.perform(get(path+"/research/suggestions")).andExpect(status().isForbidden());
        PlanForm form=PlanForm.from(plans.get(id));form.setTitle("변경된 계획");plans.update(id,form);
        mvc.perform(get(path).session(owner)).andExpect(content().string(containsString("재조사 필요")));
        assertThat(runs.findById(run.getId()).orElseThrow().isStale(plans.get(id))).isTrue();
        assertThat(run.getPlanVersion()).isZero();
        JsonNode input=research.snapshot(plans.get(id));
        assertThat(input.toString()).contains("비공개 출발역").doesNotContain("비공개 이름","기존 비공개 메모","editKeyHash");
        mvc.perform(post(path+"/research").session(owner).with(csrf())).andExpect(flash().attributeExists("researchError"));
        verify(client,times(1)).research(any());
    }
    @Test void failedResearchDoesNotBreakSavedPlan() throws Exception {
        when(client.research(any())).thenThrow(new GeminiResearchClient.ResearchFailure("할당량을 확인해주세요."));
        MockHttpSession owner=new MockHttpSession();String path=create(owner),id=path.substring(3);
        mvc.perform(post(path+"/research").session(owner).with(csrf())).andExpect(status().is3xxRedirection());
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(research.latest(id).orElseThrow().isRunning() && System.nanoTime()<deadline)Thread.sleep(20);
        assertThat(research.latest(id).orElseThrow().getStatus()).isEqualTo("FAILED");
        assertThat(research.latest(id).orElseThrow().getResultJson()).isNull();
        verify(client,times(1)).research(any());
        mvc.perform(get(path).session(owner)).andExpect(status().isOk()).andExpect(content().string(containsString("할당량을 확인해주세요")));
        mvc.perform(get(path+"/edit").session(owner)).andExpect(status().isOk());
    }
}
