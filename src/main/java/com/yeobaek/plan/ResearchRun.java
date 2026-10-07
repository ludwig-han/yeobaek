package com.yeobaek.plan;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="research_runs")
public class ResearchRun {
    @Id @Column(length=43) private String id;
    @Column(nullable=false,length=43) private String planId;
    @Column(nullable=false) private Long planVersion;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false) private Instant startedAt;
    private Instant researchedAt;
    @Column(nullable=false,length=100) private String model;
    @Column(columnDefinition="TEXT") private String resultJson;
    @Column(length=400) private String errorMessage;
    protected ResearchRun() {}
    ResearchRun(Plan plan,String model) {
        id=PlanService.token();planId=plan.getId();planVersion=plan.getVersion();
        status="RUNNING";startedAt=Instant.now();this.model=model;
    }
    void succeed(String json){resultJson=json;status="SUCCEEDED";researchedAt=Instant.now();}
    void fail(String message){status="FAILED";errorMessage=message;}
    public String getId(){return id;}
    public String getPlanId(){return planId;}
    public Long getPlanVersion(){return planVersion;}
    public String getStatus(){return status;}
    public Instant getStartedAt(){return startedAt;}
    public Instant getResearchedAt(){return researchedAt;}
    public String getModel(){return model;}
    public String getResultJson(){return resultJson;}
    public String getErrorMessage(){return errorMessage;}
    public boolean isRunning(){return "RUNNING".equals(status);}
    public boolean isSucceeded(){return "SUCCEEDED".equals(status);}
    public boolean isStale(Plan plan){return !planVersion.equals(plan.getVersion());}
}
