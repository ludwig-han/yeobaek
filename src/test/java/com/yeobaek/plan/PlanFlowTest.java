package com.yeobaek.plan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:flow;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
@AutoConfigureMockMvc
class PlanFlowTest {
    @Autowired MockMvc mvc;
    @Autowired PlanService service;
    @Autowired PlanRepository repository;

    private MockHttpServletRequestBuilder validPost(String path){
        return post(path).with(csrf()).param("title","수원에서 보내는 하루").param("date","2026-10-08")
            .param("region","수원").param("anchor1","화성행궁 충분히 보기").param("anchor2","")
            .param("transport","비공개 출발지 메모")
            .param("priorities","가챠샵\n박물관\n소품샵").param("guardrails","")
            .param("backup","").param("flexible","").param("version","0");
    }
    private MvcResult create(MockHttpSession owner) throws Exception{
        return mvc.perform(validPost("/plans").session(owner)).andExpect(status().is3xxRedirection()).andReturn();
    }
    private String path(MvcResult r){return r.getResponse().getRedirectedUrl();}
    private String id(MvcResult r){return path(r).substring("/p/".length());}

    @Test void rendersFormAndExampleWithCsrf() throws Exception{
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/").param("example","suwon")).andExpect(status().isOk())
            .andExpect(content().string(containsString("화성행궁"))).andExpect(content().string(containsString("확인 필요")));
    }
    @Test void createsPersistsAndSharesWithoutPrivateTransportOrKey() throws Exception{
        MockHttpSession owner=new MockHttpSession();MvcResult r=create(owner);
        String key=(String)r.getFlashMap().get("newEditKey");
        assertThat(key).hasSize(43);assertThat(repository.findById(id(r))).isPresent();
        assertThat(repository.findById(id(r)).orElseThrow().editKeyHash()).hasSize(64).isNotEqualTo(key);
        mvc.perform(get(path(r)).session(owner)).andExpect(status().isOk())
            .andExpect(content().string(containsString("비공개 출발지 메모")));
        mvc.perform(get(path(r)+"/share")).andExpect(status().isOk())
            .andExpect(content().string(containsString("박물관")))
            .andExpect(content().string(not(containsString("비공개 출발지 메모"))))
            .andExpect(content().string(not(containsString(key))));
        mvc.perform(get(path(r))).andExpect(status().isOk()).andExpect(content().string(not(containsString("비공개 출발지 메모"))));
    }
    @Test void cannotEditWithOnlySharedUrl() throws Exception{
        MvcResult r=create(new MockHttpSession());
        mvc.perform(validPost(path(r))).andExpect(status().isForbidden());
        mvc.perform(get(path(r)+"/edit")).andExpect(view().name("unlock"));
        mvc.perform(post(path(r)+"/unlock").with(csrf()).param("editKey","wrong")).andExpect(status().isForbidden());
    }
    @Test void correctKeyRecoversEditAccessAndChangesSharedResult() throws Exception{
        MvcResult r=create(new MockHttpSession());MockHttpSession recovered=new MockHttpSession();
        mvc.perform(post(path(r)+"/unlock").session(recovered).with(csrf())
            .param("editKey",(String)r.getFlashMap().get("newEditKey"))).andExpect(status().is3xxRedirection());
        mvc.perform(get(path(r)+"/edit").session(recovered)).andExpect(status().isOk()).andExpect(view().name("form"));
        MockHttpServletRequestBuilder update=validPost(path(r)).session(recovered);
        update.param("meeting","오후 1시 수원역");
        mvc.perform(update).andExpect(status().is3xxRedirection());
        mvc.perform(get(path(r)+"/share")).andExpect(content().string(containsString("오후 1시 수원역")));
    }
    @Test void rejectsStaleEditsWithoutOverwriting() throws Exception{
        MockHttpSession owner=new MockHttpSession();MvcResult r=create(owner);
        mvc.perform(validPost(path(r)).session(owner)).andExpect(status().is3xxRedirection());
        mvc.perform(validPost(path(r)).session(owner)).andExpect(status().isConflict())
            .andExpect(content().string(containsString("다른 화면에서 계획이 변경됐어요")));
        assertThat(repository.findById(id(r)).orElseThrow().getVersion()).isEqualTo(1L);
    }
    @Test void rejectsInvalidInputAndMissingCsrf() throws Exception{
        long before=repository.count();
        mvc.perform(post("/plans").with(csrf()).param("title","").param("date","2026-10-08").param("anchor1",""))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/plans")).andExpect(status().isForbidden());
        assertThat(repository.count()).isEqualTo(before);
    }
    @Test void blankOptionalFieldsStayValid() throws Exception{
        mvc.perform(post("/plans").with(csrf()).param("title","즉흥 나들이").param("date","2026-10-08").param("anchor1","같이 맛있는 것 먹기"))
            .andExpect(status().is3xxRedirection());
    }
    @Test void escapesUserContent() throws Exception{
        PlanForm f=PlanForm.suwon();f.setTitle("<script>alert(1)</script>");
        String id=service.create(f).plan().getId();
        mvc.perform(get("/p/"+id+"/share")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;")));
    }
    @Test void unknownPlanIs404() throws Exception{
        mvc.perform(get("/p/not-a-token")).andExpect(status().isNotFound());
        mvc.perform(get("/p/"+PlanService.token())).andExpect(status().isNotFound());
    }
}
