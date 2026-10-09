package com.yeobaek.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative metadata from source-linked evidence, never from model memory. */
final class ResearchSources {
    private ResearchSources() {}
    static final int OLD_AFTER_DAYS=180;

    static void enrich(ArrayNode sources,ArrayNode evidence,JsonNode metadata,Instant checkedAt) {
        for(int i=0;i<sources.size();i++) {
            ObjectNode source=(ObjectNode)sources.get(i);
            source.put("sourceType",isNaver(source.path("url").asText()) ? "네이버 블로그" : "유형 미확인");
            source.put("publishedDate","").put("dateBasis","").put("promotionBasis","").put("typeBasis","");
            source.put("checkedAt",checkedAt.toString()).put("old",false);
            for(JsonNode meta:metadata) {
                if(!meta.path("sourceId").isIntegralNumber() || meta.path("sourceId").asInt()!=i)continue;
                String typeQuote=quote(meta,"type",i,evidence);
                String type=meta.path("type").asText();
                if(!isNaver(source.path("url").asText()) && !typeQuote.isEmpty()) {
                    if(type.equals("NAVER_BLOG") && typeQuote.contains("네이버 블로그"))source.put("sourceType","네이버 블로그").put("typeBasis",typeQuote);
                    else if(type.equals("OFFICIAL") && typeQuote.contains("공식"))source.put("sourceType","공식").put("typeBasis",typeQuote);
                    else if(type.equals("REVIEW") && (typeQuote.contains("후기") || typeQuote.contains("방문기")))source.put("sourceType","기타 후기").put("typeBasis",typeQuote);
                }
                String dateQuote=quote(meta,"date",i,evidence),date=meta.path("publishedDate").asText();
                try {
                    LocalDate published=LocalDate.parse(date),checked=checkedAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
                    // Require a source-specific, explicit publication label and literal date.
                    if(!published.isAfter(checked) && dateQuote.contains(date) &&
                        Pattern.compile("게시일|작성일|발행일|published",Pattern.CASE_INSENSITIVE).matcher(dateQuote).find()) {
                        source.put("publishedDate",date).put("dateBasis",dateQuote);
                        source.put("old",published.isBefore(checked.minusDays(OLD_AFTER_DAYS)));
                    }
                }catch(DateTimeException ignored){ /* Unknown is safer than an invented date. */ }
                String promotion=quote(meta,"promotion",i,evidence);
                if(Pattern.compile("협찬|제공받|제공 받|원고료|유료광고|유료 광고|sponsored",Pattern.CASE_INSENSITIVE).matcher(promotion).find()
                    && !Pattern.compile("없|아니|않|미확인|불명|not |no ",Pattern.CASE_INSENSITIVE).matcher(promotion).find())
                    source.put("promotionBasis",promotion);
            }
        }
    }

    private static String quote(JsonNode meta,String field,int sourceId,ArrayNode evidence) {
        JsonNode id=meta.path(field+"EvidenceId");String quote=meta.path(field+"Quote").asText("");
        if(!id.isIntegralNumber() || id.asInt()<0 || id.asInt()>=evidence.size() || quote.isBlank() || quote.length()>240)return "";
        JsonNode e=evidence.get(id.asInt());
        // A combined citation span cannot establish which source supplied metadata.
        if(e.path("sourceIds").size()!=1 || e.path("sourceIds").get(0).asInt()!=sourceId || !e.path("text").asText().contains(quote))return "";
        return quote;
    }

    private static boolean isNaver(String url) {
        try {String host=URI.create(url).getHost();return "blog.naver.com".equalsIgnoreCase(host) || "m.blog.naver.com".equalsIgnoreCase(host);}
        catch(Exception e){return false;}
    }

    static void defaults(ObjectNode report,Instant checkedAt) {
        for(JsonNode n:report.path("sources")) {
            ObjectNode s=(ObjectNode)n;
            if(!s.has("sourceType"))s.put("sourceType",isNaver(s.path("url").asText()) ? "네이버 블로그" : "유형 미확인");
            for(String field:List.of("publishedDate","dateBasis","promotionBasis","typeBasis"))if(!s.has(field))s.put(field,"");
            if(!s.has("checkedAt"))s.put("checkedAt",checkedAt==null ? "조사 시각 미확인" : checkedAt.toString());
            if(!s.has("old"))s.put("old",false);
        }
    }
}
