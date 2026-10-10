package com.yeobaek.plan;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.*;
import static com.yeobaek.plan.PlanDetails.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class PlanForm {
    @NotBlank(message="계획 제목을 적어주세요.") @Size(max=100) private String title="";
    @NotNull(message="날짜를 골라주세요.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate date;
    @Size(max=100) private String region="";

    @Valid @NotNull @Size(min=1,max=2) private List<Anchor> anchors=new ArrayList<>(List.of(new Anchor(),new Anchor()));
    @Valid @NotNull @Size(max=6) private List<Candidate> candidates=new ArrayList<>();
    @Valid @NotNull @Size(max=4) private List<Participant> participants=new ArrayList<>();
    @Valid @NotNull private Constraints constraints=new Constraints();
    public PlanForm(){ for(int i=0;i<3;i++)candidates.add(new Candidate()); for(int i=0;i<4;i++)participants.add(defaultParticipant(i)); }
    private static Participant defaultParticipant(int index){Participant p=new Participant();p.setLabel(String.valueOf((char)('A'+index)));return p;}
    public List<Anchor> getAnchors(){return anchors;} public void setAnchors(List<Anchor> value){anchors=value;}
    public List<Candidate> getCandidates(){return candidates;} public void setCandidates(List<Candidate> value){candidates=value;}
    public List<Participant> getParticipants(){return participants;} public void setParticipants(List<Participant> value){participants=value;}
    public Constraints getConstraints(){return constraints;} public void setConstraints(Constraints value){constraints=value;}
    @AssertTrue(message="가장 중요한 목표 하나를 적어주세요.")
    public boolean isFirstAnchorPresent(){return anchors!=null && !anchors.isEmpty() && anchors.get(0)!=null && anchors.get(0).getName()!=null && !anchors.get(0).getName().isBlank();}

    @Size(max=1000) private String meeting="";
    @Size(max=3000) private String transport="";
    @Size(max=3000) private String priorities="";
    @Size(max=3000) private String guardrails="";
    @Size(max=3000) private String backup="";
    @Size(max=3000) private String flexible="";
    @NotNull private Long version=0L;

    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;}
    public String getRegion(){return region;} public void setRegion(String v){region=v;}
    public String getAnchor1(){return anchors.get(0).getName();} public void setAnchor1(String v){anchors.get(0).setName(v);}
    public String getAnchor2(){return anchors.size()>1?anchors.get(1).getName():"";} public void setAnchor2(String v){while(anchors.size()<2)anchors.add(new Anchor());anchors.get(1).setName(v);}
    public String getMeeting(){return meeting;} public void setMeeting(String v){meeting=v;}
    public String getTransport(){return transport;} public void setTransport(String v){transport=v;}
    public String getPriorities(){return priorities;} public void setPriorities(String v){priorities=v;}
    public String getGuardrails(){return guardrails;} public void setGuardrails(String v){guardrails=v;}
    public String getBackup(){return backup;} public void setBackup(String v){backup=v;}
    public String getFlexible(){return flexible;} public void setFlexible(String v){flexible=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}

    static PlanForm from(Plan p){
        PlanForm f=new PlanForm(); f.title=p.getTitle(); f.date=p.getDate(); f.region=p.getRegion();
        f.anchors.clear();p.getAnchors().forEach(a->f.anchors.add(new Anchor(a)));while(f.anchors.size()<2)f.anchors.add(new Anchor());
        f.candidates.clear();p.getCandidates().forEach(a->f.candidates.add(new Candidate(a)));while(f.candidates.size()<3)f.candidates.add(new Candidate());
        f.participants.clear();p.getParticipants().forEach(a->f.participants.add(new Participant(a)));while(f.participants.size()<4)f.participants.add(defaultParticipant(f.participants.size()));
        for(int i=0;i<f.participants.size();i++)if(f.participants.get(i).getLabel()==null || f.participants.get(i).getLabel().isBlank())f.participants.get(i).setLabel(String.valueOf((char)('A'+i)));
        f.constraints=new Constraints(p.getConstraints()); f.meeting=p.getMeeting();
        f.transport=p.getTransport(); f.priorities=p.getPriorities(); f.guardrails=p.getGuardrails();
        f.backup=p.getBackup(); f.flexible=p.getFlexible(); f.version=p.getVersion(); return f;
    }
    static PlanForm suwon(){
        PlanForm f=new PlanForm(); f.title="수원에서 보내는 하루"; f.date=LocalDate.of(2026,10,8); f.region="수원 · 행궁동";
        f.setAnchor1("화성행궁을 충분히, 자세히 관람하기"); f.setAnchor2("맛있는 음식 먹기");
        Anchor a=f.anchors.get(0); a.setKind(Kind.PLACE_VISIT);a.setPlace("화성행궁");a.setPlaceRule(PlaceRule.EXACT);a.setTimeSensitive(TimeRule.OPENING_HOURS);a.setStayImportant(Choice.YES);
        Anchor b=f.anchors.get(1); b.setKind(Kind.FOOD);b.setPlace("로우파이브");b.setPlaceRule(PlaceRule.REPLACEABLE);b.setBackupNeeded(Choice.YES);
        String[] names={"박물관","가챠샵","소재자카숍"};for(int i=0;i<3;i++){f.candidates.get(i).setName(names[i]);f.candidates.get(i).setKind(i==0?Kind.PLACE_VISIT:Kind.SHOPPING);}
        f.constraints.setMaxWaitMinutes(60);
        f.priorities="";
        f.transport="각자 출발지와 이동시간 확인\n중간 합류 / 현지 합류 중 결정";
        f.guardrails="[확인 필요] 화성행궁 관람시간·마지막 입장\n[확인 필요] 로우파이브 영업·브레이크타임·예약\n[확인 필요] 가챠샵·박물관·소품샵 운영시간\n[확인 필요] 날씨·걷는 양·귀가 교통";
        f.backup="식사 후보 A: 로우파이브\n휴무·재료 소진·웨이팅 60분 이상이면 다른 식당으로\n대안 식당: 아직 정하지 않음";
        f.flexible="행궁에서 머무를 시간\n카페에 갈지\n정확한 귀가 시각\n세세한 산책 동선"; return f;
    }
}
