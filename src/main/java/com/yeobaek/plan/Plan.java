package com.yeobaek.plan;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="plans")
public class Plan {
    @Id @Column(length=43) private String id;
    @Column(nullable=false,length=64) private String editKeyHash;
    @Column(nullable=false,length=100) private String title;
    @Column(name="plan_date",nullable=false) private LocalDate date;
    @Column(nullable=false,length=100) private String region;
    @Column(nullable=false,length=300) private String anchor1;
    @Column(nullable=false,length=300) private String anchor2;
    @Column(nullable=false,length=1000) private String meeting;
    @Column(nullable=false,length=3000) private String transport;
    @Column(nullable=false,length=3000) private String priorities;
    @Column(nullable=false,length=3000) private String guardrails;
    @Column(nullable=false,length=3000) private String backup;
    @Column(nullable=false,length=3000) private String flexible;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @Version private Long version;

    protected Plan(){}
    Plan(String id,String hash,PlanForm f){this.id=id;editKeyHash=hash;createdAt=Instant.now();apply(f);}
    void apply(PlanForm f){
        title=clean(f.getTitle()); date=f.getDate(); region=clean(f.getRegion());
        anchor1=clean(f.getAnchor1());anchor2=clean(f.getAnchor2());meeting=clean(f.getMeeting());
        transport=clean(f.getTransport());priorities=clean(f.getPriorities());guardrails=clean(f.getGuardrails());
        backup=clean(f.getBackup());flexible=clean(f.getFlexible());updatedAt=Instant.now();
    }
    private static String clean(String s){return s==null?"":s.strip();}
    String editKeyHash(){return editKeyHash;}
    public String getId(){return id;} public String getTitle(){return title;}
    public LocalDate getDate(){return date;} public String getRegion(){return region;}
    public String getAnchor1(){return anchor1;} public String getAnchor2(){return anchor2;}
    public String getMeeting(){return meeting;} public String getTransport(){return transport;}
    public String getPriorities(){return priorities;} public String getGuardrails(){return guardrails;}
    public String getBackup(){return backup;} public String getFlexible(){return flexible;}
    public Instant getUpdatedAt(){return updatedAt;} public Long getVersion(){return version;}
}
