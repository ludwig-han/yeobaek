package com.yeobaek.plan;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class PlanForm {
    @NotBlank(message="계획 제목을 적어주세요.") @Size(max=100) private String title="";
    @NotNull(message="날짜를 골라주세요.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate date;
    @Size(max=100) private String region="";
    @NotBlank(message="가장 중요한 목표 하나를 적어주세요.") @Size(max=300) private String anchor1="";
    @Size(max=300) private String anchor2="";
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
    public String getAnchor1(){return anchor1;} public void setAnchor1(String v){anchor1=v;}
    public String getAnchor2(){return anchor2;} public void setAnchor2(String v){anchor2=v;}
    public String getMeeting(){return meeting;} public void setMeeting(String v){meeting=v;}
    public String getTransport(){return transport;} public void setTransport(String v){transport=v;}
    public String getPriorities(){return priorities;} public void setPriorities(String v){priorities=v;}
    public String getGuardrails(){return guardrails;} public void setGuardrails(String v){guardrails=v;}
    public String getBackup(){return backup;} public void setBackup(String v){backup=v;}
    public String getFlexible(){return flexible;} public void setFlexible(String v){flexible=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}

    static PlanForm from(Plan p){
        PlanForm f=new PlanForm(); f.title=p.getTitle(); f.date=p.getDate(); f.region=p.getRegion();
        f.anchor1=p.getAnchor1(); f.anchor2=p.getAnchor2(); f.meeting=p.getMeeting();
        f.transport=p.getTransport(); f.priorities=p.getPriorities(); f.guardrails=p.getGuardrails();
        f.backup=p.getBackup(); f.flexible=p.getFlexible(); f.version=p.getVersion(); return f;
    }
    static PlanForm suwon(){
        PlanForm f=new PlanForm(); f.title="수원에서 보내는 하루"; f.date=LocalDate.of(2026,10,8); f.region="수원 · 행궁동";
        f.anchor1="화성행궁을 충분히, 자세히 관람하기"; f.anchor2="맛있는 음식 먹기 — 특정 식당이 아니어도 좋아요";
        f.priorities="가챠샵\n박물관\n소품샵 · 소재자카숍\n디저트 후보 · 츄러스집";
        f.transport="각자 출발지와 이동시간 확인\n중간 합류 / 현지 합류 중 결정";
        f.guardrails="[확인 필요] 화성행궁 관람시간·마지막 입장\n[확인 필요] 로우파이브 영업·브레이크타임·예약\n[확인 필요] 가챠샵·박물관·소품샵 운영시간\n[확인 필요] 날씨·걷는 양·귀가 교통";
        f.backup="식사 후보 A: 로우파이브\n휴무·재료 소진·웨이팅 60분 이상이면 다른 식당으로\n대안 식당: 아직 정하지 않음";
        f.flexible="행궁에서 머무를 시간\n카페에 갈지\n정확한 귀가 시각\n세세한 산책 동선"; return f;
    }
}
