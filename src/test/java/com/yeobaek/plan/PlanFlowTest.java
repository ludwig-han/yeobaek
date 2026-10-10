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

    @Test void participantDefaultsDoNotCreatePhantomPeopleAndNicknamesPersist() throws Exception {
        PlanForm form=new PlanForm();
        assertThat(form.getParticipants()).extracting(PlanDetails.Participant::getLabel).containsExactly("A","B","C","D");
        MockHttpSession owner=new MockHttpSession();MvcResult empty=create(owner);
        assertThat(service.get(id(empty)).getParticipants()).isEmpty();
        MvcResult named=mvc.perform(validPost("/plans").session(owner).param("participants[0].origin","수원역")
            .param("participants[1].label","친구").param("participants[1].origin","서울역"))
            .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(service.get(id(named)).getParticipants()).extracting(PlanDetails.Participant::getLabel).containsExactly("A","친구");
        mvc.perform(get(path(named)+"/edit").session(owner)).andExpect(content().string(containsString("value=\"친구\"")));
        mvc.perform(get("/")).andExpect(content().string(not(containsString("남겨둘 여백"))))
            .andExpect(content().string(containsString("value=\"D\"")));
        mvc.perform(get(path(empty))).andExpect(content().string(not(containsString("남겨둔 여백"))));
        PlanForm legacy=PlanForm.suwon();var plan=service.create(legacy).plan();
        PlanForm edit=PlanForm.from(service.get(plan.getId()));service.update(plan.getId(),edit);
        assertThat(service.get(plan.getId()).getFlexible()).isEqualTo(legacy.getFlexible());
    }

    @Test void v02FixedTimeAndEssentialPlacePersistWithoutClassification() throws Exception {
        MockHttpSession owner=new MockHttpSession();
        MvcResult r=mvc.perform(validPost("/plans").session(owner).param("anchors[0].place","예약한 식당")
            .param("anchors[0].placeEssential","true").param("anchors[0].placeRule","UNKNOWN")
            .param("anchors[0].fixedTime","18:30"))
            .andExpect(status().is3xxRedirection()).andReturn();
        PlanDetails.Anchor a=service.get(id(r)).getAnchors().get(0);
        assertThat(a.getFixedTime()).isEqualTo(java.time.LocalTime.of(18,30));
        assertThat(a.getPlaceRule()).isEqualTo(PlanDetails.PlaceRule.EXACT);
        assertThat(a.getKind()).isEqualTo(PlanDetails.Kind.UNKNOWN);
        mvc.perform(get(path(r)+"/share")).andExpect(content().string(containsString("정해진 시각 · 18:30")));
        mvc.perform(validPost(path(r)).session(owner).param("anchors[0].place","예약한 식당")
            .param("_anchors[0].placeEssential","on").param("anchors[0].placeRule","EXACT").param("anchors[0].fixedTime",""))
            .andExpect(status().is3xxRedirection());
        a=service.get(id(r)).getAnchors().get(0);
        assertThat(a.getFixedTime()).isNull();assertThat(a.getPlaceRule()).isEqualTo(PlanDetails.PlaceRule.REPLACEABLE);
        mvc.perform(validPost("/plans").param("anchors[0].fixedTime","25:00")).andExpect(status().isUnprocessableEntity());
    }

    @Test void v02LimitsNewCandidatesButPreservesSixLegacyCandidatesAndTimes() throws Exception {
        var request=validPost("/plans");for(int i=0;i<4;i++)request.param("candidates["+i+"].name","후보"+i);
        mvc.perform(request).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/")).andExpect(content().string(not(containsString("candidates[3].name"))))
            .andExpect(content().string(not(containsString("시간 제약이 있나요?"))))
            .andExpect(content().string(not(containsString("anchors[0].earliest"))));
        PlanForm f=PlanForm.suwon();
        while(f.getCandidates().size()<6){var c=new PlanDetails.Candidate();c.setName("기존 후보"+f.getCandidates().size());f.getCandidates().add(c);}
        f.getAnchors().get(0).setEarliest(java.time.LocalTime.of(19,30));f.getAnchors().get(0).setLatest(java.time.LocalTime.of(23,33));
        var created=service.create(f);String path="/p/"+created.plan().getId();MockHttpSession owner=new MockHttpSession();
        mvc.perform(post(path+"/unlock").session(owner).with(csrf()).param("editKey",created.editKey())).andExpect(status().is3xxRedirection());
        mvc.perform(get(path+"/edit").session(owner)).andExpect(status().isOk())
            .andExpect(content().string(containsString("candidates[5].name")))
            .andExpect(content().string(containsString("이전에 저장한 시간 조건")));
        var update=validPost(path).session(owner).param("anchors[0].earliest","19:30").param("anchors[0].latest","23:33");
        for(int i=0;i<6;i++)update.param("candidates["+i+"].name",f.getCandidates().get(i).getName());
        mvc.perform(update).andExpect(status().is3xxRedirection());
        Plan saved=service.get(created.plan().getId());assertThat(saved.getCandidates()).hasSize(6);
        assertThat(saved.getAnchors().get(0).getEarliest()).isEqualTo(java.time.LocalTime.of(19,30));
        assertThat(saved.getAnchors().get(0).getLatest()).isEqualTo(java.time.LocalTime.of(23,33));
        assertThat(saved.getAnchors().get(0).getFixedTime()).isNull();
    }

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
    @Test void browserLibraryRestoresFreshSessionAndNeverRemembersSharedPlans() throws Exception {
        MvcResult created=create(new MockHttpSession());
        String key=(String)created.getFlashMap().get("newEditKey");
        MockHttpSession fresh=new MockHttpSession();
        mvc.perform(post(path(created)+"/unlock").param("editKey",key).param("viewPlan","true"))
            .andExpect(status().isForbidden());
        mvc.perform(post(path(created)+"/unlock").with(csrf()).param("editKey","wrong").param("viewPlan","true"))
            .andExpect(status().isForbidden()).andExpect(flash().attributeCount(0));
        MvcResult restored=mvc.perform(post(path(created)+"/unlock").session(fresh).with(csrf())
            .param("editKey",key).param("viewPlan","true"))
            .andExpect(redirectedUrl(path(created))).andExpect(flash().attribute("rememberEditKey",key)).andReturn();
        mvc.perform(get(path(created)).session(fresh).flashAttrs(restored.getFlashMap()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("data-remember-plan")))
            .andExpect(content().string(containsString("data-key=\""+key+"\"")));
        mvc.perform(get(path(created)+"/share").session(fresh))
            .andExpect(content().string(not(containsString("data-remember-plan"))))
            .andExpect(content().string(not(containsString(key))));
        mvc.perform(get(path(created))).andExpect(content().string(not(containsString("data-remember-plan"))));
        mvc.perform(get("/")).andExpect(content().string(containsString("library-open-form")));
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
    @Test void eveningTimesAndUnusedAnchorDoNotTriggerMisleadingRangeError() throws Exception {
        mvc.perform(validPost("/plans").param("anchors[0].timeSensitive","NONE")
            .param("anchors[0].earliest","19:30").param("anchors[0].latest","23:33")
            .param("anchors[1].timeSensitive","FIXED_TIME"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(validPost("/plans").param("anchors[0].earliest","23:33").param("anchors[0].latest","19:30"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("anchors[0].timeSensitive","FIXED_TIME"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string(containsString("시작 또는 종료 시각")));
        mvc.perform(validPost("/plans").param("anchors[0].earliest","").param("anchors[0].latest",""))
            .andExpect(status().is3xxRedirection());
    }
    @Test void lastTrainAndWalkingPreferencesPersistWithoutSharingDestination() throws Exception {
        MockHttpSession owner=new MockHttpSession();
        MvcResult r=mvc.perform(validPost("/plans").session(owner)
            .param("constraints.returnMode","LAST_TRAIN").param("constraints.returnDestination","비공개 귀가역")
            .param("constraints.walking","REDUCE").param("constraints.walkingNote","하루 1만 보 이내"))
            .andExpect(status().is3xxRedirection()).andReturn();
        Plan p=service.get(id(r));
        assertThat(p.getConstraints().getReturnMode()).isEqualTo(PlanDetails.ReturnMode.LAST_TRAIN);
        assertThat(p.getConstraints().getWalkingNote()).isEqualTo("하루 1만 보 이내");
        mvc.perform(get(path(r)).session(owner)).andExpect(content().string(containsString("비공개 귀가역")));
        mvc.perform(get(path(r)+"/share")).andExpect(content().string(not(containsString("비공개 귀가역"))))
            .andExpect(content().string(containsString("막차를 놓치지 않고 귀가")));
        mvc.perform(validPost("/plans").param("constraints.returnMode","BY_TIME"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("constraints.returnMode","BY_TIME").param("constraints.returnBy","23:33"))
            .andExpect(status().is3xxRedirection());
    }

    @Test void structuredDecisionsPersistAndPrivateOriginsNeverAppearInSharedHtml() throws Exception {
        MockHttpSession owner=new MockHttpSession();
        MvcResult r=mvc.perform(post("/plans").session(owner).with(csrf())
            .param("title","구조화 계획").param("date","2026-10-08")
            .param("anchors[0].name","행궁 제대로 보기").param("anchors[0].kind","PLACE_VISIT")
            .param("anchors[0].place","화성행궁").param("anchors[0].placeRule","EXACT")
            .param("anchors[0].stayMinutes","120").param("anchors[0].stayImportant","YES")
            .param("candidates[0].name","박물관").param("candidates[0].importance","HIGH")
            .param("participants[0].label","참여자 A").param("participants[0].origin","비공개 출발역")
            .param("constraints.maxWaitMinutes","30").param("constraints.fairTravel","true"))
            .andExpect(status().is3xxRedirection()).andReturn();
        Plan plan=service.get(id(r));
        assertThat(plan.getAnchors().get(0).getPlaceRule()).isEqualTo(PlanDetails.PlaceRule.EXACT);
        assertThat(plan.getAnchors().get(0).getStayMinutes()).isEqualTo(120);
        assertThat(plan.getConstraints().getMaxWaitMinutes()).isEqualTo(30);
        assertThat(plan.getConstraints().isFairTravel()).isTrue();
        mvc.perform(get(path(r)+"/share").session(owner)).andExpect(status().isOk())
            .andExpect(content().string(containsString("화성행궁")))
            .andExpect(content().string(not(containsString("비공개 출발역"))))
            .andExpect(content().string(not(containsString("참여자 A"))));
        mvc.perform(get(path(r)+"/edit").session(owner)).andExpect(status().isOk())
            .andExpect(content().string(containsString("비공개 출발역")))
            .andExpect(content().string(containsString("value=\"120\"")));
        PlanForm updated=PlanForm.from(plan);updated.getAnchors().get(0).setStayMinutes(90);
        service.update(id(r),updated);
        assertThat(service.get(id(r)).getAnchors().get(0).getStayMinutes()).isEqualTo(90);
        assertThat(service.get(id(r)).getVersion()).isEqualTo(1L);
    }

    @Test void rejectsRequiredPlaceWithoutNameAndInvalidConstraints() throws Exception {
        mvc.perform(validPost("/plans").param("anchors[0].placeRule","EXACT"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("constraints.maxWaitMinutes","-1"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("anchors[0].earliest","18:00").param("anchors[0].latest","10:00"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("anchors[0].kind","INVENTED"))
            .andExpect(status().isUnprocessableEntity());
        mvc.perform(validPost("/plans").param("anchors[2].name","세 번째 목표"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test void keepsDifferentAnchorDecisionsIndependent() throws Exception {
        MvcResult r=mvc.perform(post("/plans").with(csrf())
            .param("title","서로 다른 목표").param("date","2026-10-08")
            .param("anchors[0].name","행궁 관람").param("anchors[0].kind","PLACE_VISIT")
            .param("anchors[0].place","화성행궁").param("anchors[0].placeRule","EXACT")
            .param("anchors[1].name","맛있는 식사").param("anchors[1].kind","FOOD")
            .param("anchors[1].place","로우파이브").param("anchors[1].placeRule","REPLACEABLE"))
            .andExpect(status().is3xxRedirection()).andReturn();
        Plan plan=service.get(id(r));
        assertThat(plan.getAnchors().get(0).getKind()).isEqualTo(PlanDetails.Kind.PLACE_VISIT);
        assertThat(plan.getAnchors().get(0).getPlaceRule()).isEqualTo(PlanDetails.PlaceRule.EXACT);
        assertThat(plan.getAnchors().get(1).getKind()).isEqualTo(PlanDetails.Kind.FOOD);
        assertThat(plan.getAnchors().get(1).getPlaceRule()).isEqualTo(PlanDetails.PlaceRule.REPLACEABLE);
    }
}
