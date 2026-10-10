package com.yeobaek.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Read-time compatibility: never invent sentence-level attribution for an old paragraph. */
final class ResearchPresentation {
    static final List<String> SECTIONS=List.of("anchors","risks","backups","priorities","unknown","flexible");
    static void prepare(ObjectNode report) {
        for(String section:SECTIONS)for(JsonNode n:report.path(section)) {
            ObjectNode item=(ObjectNode)n;
            if(!item.has("claims")) {
                ObjectNode claim=item.putArray("claims").addObject().put("text",item.path("detail").asText());
                claim.set("sourceIds",item.path("sourceIds").deepCopy());
            }
        }
        ArrayNode summary=report.putArray("summary");Set<String> selected=new HashSet<>();
        for(JsonNode ref:report.path("highlights")) {
            String section=ref.path("section").asText();int index=ref.path("itemIndex").asInt(-1),claim=ref.path("claimIndex").asInt(-1);
            if(!List.of("anchors","risks","unknown").contains(section) || index<0 || index>=report.path(section).size())continue;
            JsonNode item=report.path(section).get(index);
            if(claim<0 || claim>=item.path("claims").size() || summary.size()>=3 || !selected.add(section+":"+index+":"+claim))continue;
            add(summary,item,claim);
        }
        if(!summary.isEmpty())return;
        // Old reports have no authored highlights: surface existing text without a new model call.
        if(!report.path("risks").isEmpty())add(summary,report.path("risks").get(0),0);
        Set<Integer> anchors=new HashSet<>();
        for(JsonNode item:report.path("anchors")) {
            if(summary.size()>=3)break;
            if(anchors.add(item.path("anchorIndex").asInt(-1)))add(summary,item,0);
        }
        if(summary.isEmpty())for(JsonNode item:report.path("unknown")){if(summary.size()>=3)break;add(summary,item,0);}
    }
    private static void add(ArrayNode summary,JsonNode item,int claimIndex) {
        if(item.path("claims").isEmpty())return;
        ObjectNode copy=((ObjectNode)item).deepCopy();
        copy.putArray("claims").add(item.path("claims").get(claimIndex).deepCopy());summary.add(copy);
    }
}
