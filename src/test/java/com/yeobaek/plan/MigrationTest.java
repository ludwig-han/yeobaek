package com.yeobaek.plan;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {
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
