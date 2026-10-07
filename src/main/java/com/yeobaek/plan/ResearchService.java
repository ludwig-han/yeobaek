package com.yeobaek.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** A single in-process worker; no external queue or long-running DB transaction. */
@Service
public class ResearchService {
    private final ResearchRunRepository runs;
    private final GeminiResearchClient client;
    private final ObjectMapper mapper;
    private final int dailyLimit;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"plan-research");t.setDaemon(true);return t;});
    private boolean busy;
    public ResearchService(ResearchRunRepository runs,GeminiResearchClient client,ObjectMapper mapper,
            @Value("${research.daily-limit:3}") int dailyLimit){this.runs=runs;this.client=client;this.mapper=mapper;this.dailyLimit=dailyLimit;}
    public boolean available(){return client.available();}
    public Optional<ResearchRun> latest(String id) {
        Optional<ResearchRun> run=runs.findFirstByPlanIdOrderByStartedAtDesc(id);
        run.filter(r->r.isRunning() && r.getStartedAt().isBefore(Instant.now().minusSeconds(240))).ifPresent(r->{
            r.fail("서버 재시작 또는 시간 초과로 조사가 중단됐습니다. 다시 시도해주세요.");runs.save(r);
        });return run;
    }
    public synchronized ResearchRun start(Plan plan) {
        if(!available())throw new GeminiResearchClient.ResearchFailure("Gemini 키·모델·조사 활성화 설정이 필요합니다.");
        if(busy)throw new GeminiResearchClient.ResearchFailure("다른 조사가 진행 중입니다. 완료 후 다시 눌러주세요.");
        Optional<ResearchRun> previous=latest(plan.getId());
        if(previous.isPresent() && (previous.get().isRunning() || previous.get().getStartedAt().isAfter(Instant.now().minusSeconds(60))))
            throw new GeminiResearchClient.ResearchFailure("방금 조사를 요청했습니다. 잠시 기다려주세요.");
        Instant day=LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        if(runs.countByStartedAtAfter(day)>=dailyLimit)throw new GeminiResearchClient.ResearchFailure("오늘의 앱 조사 횟수 한도에 도달했습니다.");
        JsonNode input=snapshot(plan);ResearchRun run=runs.saveAndFlush(new ResearchRun(plan,client.model()));busy=true;
        try{worker.submit(()->execute(run.getId(),input));}
        catch(RejectedExecutionException e){busy=false;run.fail("서버 종료 중입니다. 다시 시도해주세요.");runs.save(run);}
        return run;
    }
    void execute(String id,JsonNode input) {
        try {
            JsonNode report=client.research(input);
            ResearchRun run=runs.findById(id).orElseThrow();
            if(run.isRunning()){run.succeed(mapper.writeValueAsString(report));runs.save(run);}
        }catch(Exception e){
            runs.findById(id).ifPresent(run->{run.fail(e instanceof GeminiResearchClient.ResearchFailure?e.getMessage():"조사 처리 중 문제가 생겼습니다. 다시 시도해주세요.");runs.save(run);});
        }finally{synchronized(this){busy=false;}}
    }
    JsonNode snapshot(Plan p) {
        ObjectNode input=mapper.createObjectNode();input.put("title",p.getTitle());input.put("date",p.getDate().toString());input.put("region",p.getRegion());
        input.put("checkedAt",Instant.now().toString());input.set("anchors",mapper.valueToTree(p.getAnchors()));
        input.set("priorities",mapper.valueToTree(p.getCandidates()));input.set("guardrailAndTransport",mapper.valueToTree(p.getConstraints()));
        // Anonymize participants; never send edit credentials or old private free-form notes.
        input.set("departureStations",mapper.valueToTree(p.getParticipants().stream().map(PlanDetails.Participant::getOrigin).toList()));
        input.put("flexible",p.getFlexible());return input;
    }
    public Map<String,Object> report(ResearchRun run) {
        if(!run.isSucceeded())return Map.of();
        try{return mapper.readValue(run.getResultJson(),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}
        catch(Exception e){return Map.of();}
    }
    @PreDestroy void shutdown(){worker.shutdownNow();}
}
