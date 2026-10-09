package com.yeobaek.plan;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/** Contract tests use a local HTTP server; production always calls Gemini. */
class GeminiResearchClientTest {
    final ObjectMapper mapper=new ObjectMapper();
    HttpServer server;
    List<JsonNode> requests=new ArrayList<>();
    Queue<String> responses=new ArrayDeque<>();
    int httpStatus=200;
    GeminiResearchClient client;
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/generate",exchange->{
            requests.add(mapper.readTree(exchange.getRequestBody()));
            byte[] body=responses.remove().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(httpStatus,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        client=new GeminiResearchClient(mapper,"test-key","gemini-3.8-flash",true,HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/generate"));
    }
    @AfterEach void stop(){server.stop(0);}
    String response(String text,boolean grounded) throws Exception {
        ObjectNode response=mapper.createObjectNode().put("status","completed");ArrayNode steps=response.putArray("steps");
        if(grounded){
            steps.addObject().put("type","google_search_call").putObject("arguments").putArray("queries").add("화성행궁 공식 관람안내");
            steps.addObject().put("type","google_search_result").put("is_error",false).putArray("result");
            response.putObject("usage").putArray("grounding_tool_count").addObject().put("type","google_search").put("count",1);
        }
        ObjectNode part=steps.addObject().put("type","model_output").putArray("content").addObject().put("type","text").put("text",text);
        if(grounded)part.putArray("annotations").addObject().put("type","url_citation").put("start_index",0)
            .put("end_index",text.getBytes(StandardCharsets.UTF_8).length).put("url","https://www.swcf.or.kr/?p=65").put("title","수원문화재단");
        return response.toString();
    }
    ObjectNode findings() {
        ObjectNode out=mapper.createObjectNode();for(String name:List.of("anchors","risks","backups","priorities","unknown","flexible"))out.putArray(name);
        ((ArrayNode)out.get("anchors")).addObject().put("subject","화성행궁").put("detail","관람 안내 확인")
            .put("state","SOURCED").put("anchorIndex",0).putArray("evidenceIds").add(0);return out;
    }
    @Test void performsRealSearchContractThenExtractsOnlyItsEvidence() throws Exception {
        responses.add(response("공식 관람 안내를 확인하세요.",true));responses.add(response(findings().toString(),false));
        JsonNode result=client.research(mapper.createObjectNode());
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).has("response_format")).isFalse();
        assertThat(requests.get(1).has("tools")).isFalse();
        assertThat(requests.get(0).path("input").asText()).contains("site:blog.naver.com","라스트오더","엘리베이터","날짜 미확인","EXACT");
        assertThat(requests.get(1).path("response_format").path("schema").path("properties").has("sourceMetadata")).isTrue();
        assertThat(requests.toString()).doesNotContain("tool_choice","function");
        assertThat(requests.get(0).path("model").asText()).isEqualTo("gemini-3.8-flash");
        assertThat(result.path("evidence").get(0).path("text").asText()).isEqualTo("공식 관람 안내를 확인하세요.");
        assertThat(requests.get(0).path("tools").get(0).path("type").asText()).isEqualTo("google_search");
        assertThat(requests.get(1).path("response_format").path("mime_type").asText()).isEqualTo("application/json");
        assertThat(result.path("sources").get(0).path("url").asText()).startsWith("https://www.swcf.or.kr/");
        assertThat(result.path("sources").get(0).path("id").asInt()).isZero();
        assertThat(result.path("anchors").get(0).path("state").asText()).isEqualTo("SOURCED");
    }
    @Test void rejectsMemoryOnlyAnswerBeforeExtraction() throws Exception {
        responses.add(response("운영시간은 9시입니다.",false));
        assertThatThrownBy(()->client.research(mapper.createObjectNode())).hasMessageContaining("실제 검색 근거");
        assertThat(requests).hasSize(1);
    }
    @Test void rejectsFabricatedEvidenceAndUnsafeLinks() throws Exception {
        responses.add(response("조사",true));ObjectNode f=findings();((ArrayNode)f.path("anchors").get(0).get("evidenceIds")).set(0,IntNode.valueOf(99));
        responses.add(response(f.toString(),false));
        assertThatThrownBy(()->client.research(mapper.createObjectNode())).hasMessageContaining("출처 연결");
        assertThat(GeminiResearchClient.safeUrl("javascript:alert(1)")).isFalse();
        assertThat(GeminiResearchClient.safeUrl("https://user:password@example.com")).isFalse();
    }
    @Test void neverShowsProviderErrorsOrKeysAndDoesNotRetryQuotaFailure() {
        httpStatus=429;responses.add("{\"error\":\"test-key confidential error\"}");
        assertThatThrownBy(()->client.research(mapper.createObjectNode())).hasMessageContaining("할당량").hasMessageNotContaining("test-key");
        assertThat(requests).hasSize(1);
    }
    @Test void preventsBackupReplacingExactAnchor() throws Exception {
        responses.add(response("조사",true));ObjectNode f=findings();
        ((ArrayNode)f.get("backups")).add(f.path("anchors").get(0).deepCopy());responses.add(response(f.toString(),false));
        ObjectNode input=mapper.createObjectNode();input.putArray("anchors").addObject().put("placeRule","EXACT");
        assertThat(client.research(input).path("backups").size()).isZero();
    }
    @Test void preservesConditionalBackupForReplaceableAnchorWithTwoCallsOnly() throws Exception {
        responses.add(response("식당 임시휴무 공식 공지",true));ObjectNode f=findings();
        ((ArrayNode)f.get("backups")).add(f.path("anchors").get(0).deepCopy());responses.add(response(f.toString(),false));
        ObjectNode input=mapper.createObjectNode();input.putArray("anchors").addObject().put("placeRule","REPLACEABLE");
        assertThat(client.research(input).path("backups").size()).isEqualTo(1);assertThat(requests).hasSize(2);
    }
    @Test void acceptsCitationEvidenceWithoutOtherOptionalSearchSignals() throws Exception {
        ObjectNode raw=(ObjectNode)mapper.readTree(response("한글 근거",true));
        ArrayNode steps=(ArrayNode)raw.get("steps");steps.remove(0);steps.remove(0);raw.remove("usage");
        assertThat(client.grounding(raw).path("evidence").get(0).path("text").asText()).isEqualTo("한글 근거");
    }
    @Test void rejectsSearchExecutionWithoutUsableCitationsBeforeExtraction() throws Exception {
        ObjectNode raw=(ObjectNode)mapper.readTree(response("한글 근거",true));
        ((ObjectNode)raw.path("steps").get(2).path("content").get(0)).remove("annotations");
        responses.add(raw.toString());
        assertThatThrownBy(()->client.research(mapper.createObjectNode())).hasMessageContaining("출처와 답변");
        assertThat(requests).hasSize(1);
    }
    @Test void rejectsFailedSearchAndIncompleteInteractionWithoutRetry() throws Exception {
        ObjectNode raw=(ObjectNode)mapper.readTree(response("근거",true));raw.put("status","incomplete");responses.add(raw.toString());
        assertThatThrownBy(()->client.research(mapper.createObjectNode())).hasMessageContaining("완료되지");
        assertThat(requests).hasSize(1);
        raw.put("status","completed");((ObjectNode)raw.path("steps").get(1)).put("is_error",true);
        assertThatThrownBy(()->client.grounding(raw)).hasMessageContaining("Search 실행");
    }
    @Test void rejectsBrokenUtf8OffsetsAndPreservesMultipleCitations() throws Exception {
        ObjectNode raw=(ObjectNode)mapper.readTree(response("한글 근거",true));
        ArrayNode annotations=(ArrayNode)raw.path("steps").get(2).path("content").get(0).get("annotations");
        ObjectNode original=(ObjectNode)annotations.get(0);
        annotations.add(original.deepCopy().put("url","https://example.org/official"));
        JsonNode ground=client.grounding(raw);
        assertThat(ground.path("evidence").get(0).path("sourceIds").size()).isEqualTo(2);
        assertThat(GeminiResearchClient.citedText("한글 근거",original.deepCopy().put("start_index",1))).isEmpty();
        assertThat(GeminiResearchClient.citedText("한글 근거",original.deepCopy().put("end_index",999))).isEmpty();
    }

}
