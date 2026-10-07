package com.yeobaek.plan;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static com.yeobaek.plan.PlanDetails.*;

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

    @Embedded private Constraints constraints=new Constraints();
    @ElementCollection @CollectionTable(name="plan_anchors",joinColumns=@JoinColumn(name="plan_id")) @OrderColumn(name="position")
    private List<Anchor> anchors=new ArrayList<>();
    @ElementCollection @CollectionTable(name="plan_candidates",joinColumns=@JoinColumn(name="plan_id")) @OrderColumn(name="position")
    private List<Candidate> candidates=new ArrayList<>();
    @ElementCollection @CollectionTable(name="plan_participants",joinColumns=@JoinColumn(name="plan_id")) @OrderColumn(name="position")
    private List<Participant> participants=new ArrayList<>();

    protected Plan(){}
    Plan(String id,String hash,PlanForm f){this.id=id;editKeyHash=hash;createdAt=Instant.now();apply(f);}
    void apply(PlanForm f){
        constraints=new Constraints(f.getConstraints());
        anchors.clear(); f.getAnchors().stream().filter(a -> a.getName()!=null && !a.getName().isBlank()).forEach(a -> anchors.add(new Anchor(a)));
        candidates.clear(); f.getCandidates().stream().filter(a -> a.getName()!=null && !a.getName().isBlank()).forEach(a -> candidates.add(new Candidate(a)));
        participants.clear(); f.getParticipants().stream().filter(a -> (a.getOrigin()!=null && !a.getOrigin().isBlank()) || (a.getLabel()!=null && !a.getLabel().isBlank())).forEach(a -> participants.add(new Participant(a)));
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
    public List<Anchor> getAnchors(){return anchors;}
    public List<Candidate> getCandidates(){return candidates;}
    public List<Participant> getParticipants(){return participants;}
    public Constraints getConstraints(){return constraints;}
    void initializeDetails(){anchors.size();candidates.size();participants.size();}
    public Instant getUpdatedAt(){return updatedAt;} public Long getVersion(){return version;}
}
