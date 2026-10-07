package com.yeobaek.plan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class PlanService {
    private static final Logger log=LoggerFactory.getLogger(PlanService.class);
    private static final SecureRandom RANDOM=new SecureRandom();
    private final PlanRepository repository;
    PlanService(PlanRepository repository){this.repository=repository;}
    public record Created(Plan plan,String editKey){}
    @Transactional public Created create(PlanForm form){
        String key=token(); Plan plan=repository.saveAndFlush(new Plan(token(),hash(key),form));
        log.info("plan_created"); // Do not log titles, locations, tokens or editing keys.
        return new Created(plan,key);
    }
    public Plan get(String id){
        if(!id.matches("[A-Za-z0-9_-]{43}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public boolean verifyKey(String id,String key){
        Plan p=get(id);
        if(key==null || !key.matches("[A-Za-z0-9_-]{43}")) return false;
        return MessageDigest.isEqual(p.editKeyHash().getBytes(StandardCharsets.UTF_8),hash(key).getBytes(StandardCharsets.UTF_8));
    }
    @Transactional public void update(String id,PlanForm form){
        Plan p=get(id);
        if(!p.getVersion().equals(form.getVersion())) throw new StalePlanException();
        p.apply(form); repository.flush(); log.info("plan_updated");
    }
    static String token(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static String hash(String key){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    static class StalePlanException extends RuntimeException {}
}
