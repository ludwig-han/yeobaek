package com.yeobaek.plan;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {
    @Test void upgradesV3KeepingExistingReturnAndStayPreferences() throws Exception {
        String url="jdbc:h2:mem:migrationV4;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("3").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.execute("INSERT INTO plans(id,edit_key_hash,title,plan_date,region,anchor1,anchor2,meeting,transport,priorities,guardrails,backup,flexible,created_at,updated_at,version,return_by) VALUES ('"+"c".repeat(43)+"','"+"d".repeat(64)+"','기존',DATE '2026-10-08','','목표','','','','','','','',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,TIME '23:33:00')");
            s.execute("INSERT INTO plan_anchors(plan_id,position,goal_name,stay_minutes,stay_important) VALUES ('"+"c".repeat(43)+"',0,'관람',120,'YES')");
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT return_mode,return_by FROM plans")) {
                r.next();assertThat(r.getString(1)).isEqualTo("BY_TIME");assertThat(r.getTime(2).toLocalTime()).isEqualTo(java.time.LocalTime.of(23,33));
            }
            try(var r=s.executeQuery("SELECT stay_minutes FROM plan_anchors")) {r.next();assertThat(r.getInt(1)).isEqualTo(120);}
        }
    }
    @Test void upgradesV1WithoutGuessingPreferencesOrLosingNotes() throws Exception {
        String url="jdbc:h2:mem:migration;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("1").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.execute("INSERT INTO plans VALUES ('"+"a".repeat(43)+"','"+"b".repeat(64)+"','기존 계획',DATE '2026-10-08','수원','행궁','식사','','비공개 이동','기존 후보','기존 주의','기존 대안','여백',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,4)");
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT transport,version,walking FROM plans")) {
                r.next();assertThat(r.getString(1)).isEqualTo("비공개 이동");
                assertThat(r.getLong(2)).isEqualTo(4);assertThat(r.getString(3)).isEqualTo("UNKNOWN");
            }
            try(var r=s.executeQuery("SELECT goal_name,place_rule FROM plan_anchors ORDER BY position")) {
                r.next();assertThat(r.getString(1)).isEqualTo("행궁");assertThat(r.getString(2)).isEqualTo("UNKNOWN");
                r.next();assertThat(r.getString(1)).isEqualTo("식사");assertThat(r.next()).isFalse();
            }
        }
    }
}
