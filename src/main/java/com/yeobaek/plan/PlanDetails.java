package com.yeobaek.plan;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/** User decisions; UNKNOWN is deliberately different from false. */
public final class PlanDetails {
    private PlanDetails() {}
    public enum Kind { UNKNOWN("미정"), PLACE_VISIT("장소·관람"), FOOD("식사"), ACTIVITY("활동"), SHOPPING("쇼핑"), OTHER("기타");
        private final String label; Kind(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum PlaceRule { UNKNOWN("미정"), EXACT("이 장소가 필수"), REPLACEABLE("다른 장소도 가능");
        private final String label; PlaceRule(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum Choice { UNKNOWN("미정"), YES("예"), NO("아니요");
        private final String label; Choice(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum TimeRule { UNKNOWN("미정"), NONE("없음"), OPENING_HOURS("운영시간 영향"), FIXED_TIME("특정 시각");
        private final String label; TimeRule(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum Importance { NORMAL("중요함"), HIGH("매우 중요함");
        private final String label; Importance(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum MeetingStyle { UNKNOWN("미정"), TOGETHER("같이 출발"), MIDWAY("중간에서 합류"), ON_SITE("현지에서 만나도 됨");
        private final String label; MeetingStyle(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum Walking { UNKNOWN("미정"), LOW("걷기 적게"), MEDIUM("적당히"), HIGH("많이 걸어도 괜찮음");
        private final String label; Walking(String label){this.label=label;} public String getLabel(){return label;}
    }
    public enum Weather { UNKNOWN("미정"), LOW("날씨 영향 적음"), HIGH("비·더위에 민감");
        private final String label; Weather(String label){this.label=label;} public String getLabel(){return label;}
    }
    @Embeddable
    public static class Anchor {
        @Column(name="goal_name", length=300) @Size(max=300) private String name="";
        @Column(name="kind", length=30) @Enumerated(EnumType.STRING) @NotNull private Kind kind=Kind.UNKNOWN;
        @Column(name="place_name", length=150) @Size(max=150) private String place="";
        @Column(name="place_rule", length=30) @Enumerated(EnumType.STRING) @NotNull private PlaceRule placeRule=PlaceRule.UNKNOWN;
        @Column(name="time_sensitive", length=30) @Enumerated(EnumType.STRING) @NotNull private TimeRule timeSensitive=TimeRule.UNKNOWN;
        @Column(name="earliest") @DateTimeFormat(pattern="HH:mm")  private LocalTime earliest=null;
        @Column(name="latest") @DateTimeFormat(pattern="HH:mm")  private LocalTime latest=null;
        @Column(name="stay_important", length=30) @Enumerated(EnumType.STRING) @NotNull private Choice stayImportant=Choice.UNKNOWN;
        @Column(name="stay_minutes") @Min(10) @Max(720) private Integer stayMinutes=null;
        @Column(name="backup_needed", length=30) @Enumerated(EnumType.STRING) @NotNull private Choice backupNeeded=Choice.UNKNOWN;
        @Column(name="note", length=300) @Size(max=300) private String note="";
        public Anchor() {}
        public Anchor(Anchor other) {
            this.name=other.name;
            this.kind=other.kind;
            this.place=other.place;
            this.placeRule=other.placeRule;
            this.timeSensitive=other.timeSensitive;
            this.earliest=other.earliest;
            this.latest=other.latest;
            this.stayImportant=other.stayImportant;
            this.stayMinutes=other.stayMinutes;
            this.backupNeeded=other.backupNeeded;
            this.note=other.note;
        }
        public String getName(){return name;} public void setName(String value){name=value;}
        public Kind getKind(){return kind;} public void setKind(Kind value){kind=value;}
        public String getPlace(){return place;} public void setPlace(String value){place=value;}
        public PlaceRule getPlaceRule(){return placeRule;} public void setPlaceRule(PlaceRule value){placeRule=value;}
        public TimeRule getTimeSensitive(){return timeSensitive;} public void setTimeSensitive(TimeRule value){timeSensitive=value;}
        public LocalTime getEarliest(){return earliest;} public void setEarliest(LocalTime value){earliest=value;}
        public LocalTime getLatest(){return latest;} public void setLatest(LocalTime value){latest=value;}
        public Choice getStayImportant(){return stayImportant;} public void setStayImportant(Choice value){stayImportant=value;}
        public Integer getStayMinutes(){return stayMinutes;} public void setStayMinutes(Integer value){stayMinutes=value;}
        public Choice getBackupNeeded(){return backupNeeded;} public void setBackupNeeded(Choice value){backupNeeded=value;}
        public String getNote(){return note;} public void setNote(String value){note=value;}
        @AssertTrue(message="필수 장소의 이름을 적어주세요.")
        public boolean isPlaceValid(){return name==null || name.isBlank() || placeRule!=PlaceRule.EXACT || (place!=null && !place.isBlank());}
        @AssertTrue(message="시간 범위는 시작보다 끝이 늦어야 합니다.")
        public boolean isTimeValid(){return (timeSensitive!=TimeRule.FIXED_TIME || earliest!=null || latest!=null) && (earliest==null || latest==null || earliest.isBefore(latest)) && (timeSensitive!=TimeRule.NONE || (earliest==null && latest==null));}
    }
    @Embeddable
    public static class Candidate {
        @Column(name="candidate_name", length=150) @Size(max=150) private String name="";
        @Column(name="kind", length=30) @Enumerated(EnumType.STRING) @NotNull private Kind kind=Kind.UNKNOWN;
        @Column(name="importance", length=30) @Enumerated(EnumType.STRING) @NotNull private Importance importance=Importance.NORMAL;
        @Column(name="note", length=300) @Size(max=300) private String note="";
        public Candidate() {}
        public Candidate(Candidate other) {
            this.name=other.name;
            this.kind=other.kind;
            this.importance=other.importance;
            this.note=other.note;
        }
        public String getName(){return name;} public void setName(String value){name=value;}
        public Kind getKind(){return kind;} public void setKind(Kind value){kind=value;}
        public Importance getImportance(){return importance;} public void setImportance(Importance value){importance=value;}
        public String getNote(){return note;} public void setNote(String value){note=value;}
    }
    @Embeddable
    public static class Participant {
        @Column(name="participant_label", length=50) @Size(max=50) private String label="";
        @Column(name="origin", length=150) @Size(max=150) private String origin="";
        public Participant() {}
        public Participant(Participant other) {
            this.label=other.label;
            this.origin=other.origin;
        }
        public String getLabel(){return label;} public void setLabel(String value){label=value;}
        public String getOrigin(){return origin;} public void setOrigin(String value){origin=value;}
    }
    @Embeddable
    public static class Constraints {
        @Column(name="meeting_style", length=30) @Enumerated(EnumType.STRING) @NotNull private MeetingStyle meetingStyle=MeetingStyle.UNKNOWN;
        @Column(name="short_travel")  private boolean shortTravel=false;
        @Column(name="few_transfers")  private boolean fewTransfers=false;
        @Column(name="avoid_crowds")  private boolean avoidCrowds=false;
        @Column(name="low_cost")  private boolean lowCost=false;
        @Column(name="fair_travel")  private boolean fairTravel=false;
        @Column(name="walking", length=30) @Enumerated(EnumType.STRING) @NotNull private Walking walking=Walking.UNKNOWN;
        @Column(name="max_wait_minutes") @Min(0) @Max(300) private Integer maxWaitMinutes=null;
        @Column(name="avoid_late_return", length=30) @Enumerated(EnumType.STRING) @NotNull private Choice avoidLateReturn=Choice.UNKNOWN;
        @Column(name="return_by") @DateTimeFormat(pattern="HH:mm")  private LocalTime returnBy=null;
        @Column(name="budget_per_person") @Min(0) @Max(10000000) private Integer budgetPerPerson=null;
        @Column(name="weather", length=30) @Enumerated(EnumType.STRING) @NotNull private Weather weather=Weather.UNKNOWN;
        public Constraints() {}
        public Constraints(Constraints other) {
            this.meetingStyle=other.meetingStyle;
            this.shortTravel=other.shortTravel;
            this.fewTransfers=other.fewTransfers;
            this.avoidCrowds=other.avoidCrowds;
            this.lowCost=other.lowCost;
            this.fairTravel=other.fairTravel;
            this.walking=other.walking;
            this.maxWaitMinutes=other.maxWaitMinutes;
            this.avoidLateReturn=other.avoidLateReturn;
            this.returnBy=other.returnBy;
            this.budgetPerPerson=other.budgetPerPerson;
            this.weather=other.weather;
        }
        public MeetingStyle getMeetingStyle(){return meetingStyle;} public void setMeetingStyle(MeetingStyle value){meetingStyle=value;}
        public boolean isShortTravel(){return shortTravel;} public void setShortTravel(boolean value){shortTravel=value;}
        public boolean isFewTransfers(){return fewTransfers;} public void setFewTransfers(boolean value){fewTransfers=value;}
        public boolean isAvoidCrowds(){return avoidCrowds;} public void setAvoidCrowds(boolean value){avoidCrowds=value;}
        public boolean isLowCost(){return lowCost;} public void setLowCost(boolean value){lowCost=value;}
        public boolean isFairTravel(){return fairTravel;} public void setFairTravel(boolean value){fairTravel=value;}
        public Walking getWalking(){return walking;} public void setWalking(Walking value){walking=value;}
        public Integer getMaxWaitMinutes(){return maxWaitMinutes;} public void setMaxWaitMinutes(Integer value){maxWaitMinutes=value;}
        public Choice getAvoidLateReturn(){return avoidLateReturn;} public void setAvoidLateReturn(Choice value){avoidLateReturn=value;}
        public LocalTime getReturnBy(){return returnBy;} public void setReturnBy(LocalTime value){returnBy=value;}
        public Integer getBudgetPerPerson(){return budgetPerPerson;} public void setBudgetPerPerson(Integer value){budgetPerPerson=value;}
        public Weather getWeather(){return weather;} public void setWeather(Weather value){weather=value;}
    }
}
