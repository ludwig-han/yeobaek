package com.yeobaek.plan;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlanController {
    private final PlanService service;
    PlanController(PlanService service){this.service=service;}
    @GetMapping("/") String home(@RequestParam(defaultValue="") String example,Model m){
        PlanForm f="suwon".equals(example)?PlanForm.suwon():new PlanForm();
        if(f.getDate()==null) f.setDate(LocalDate.now(ZoneId.of("Asia/Seoul")));
        m.addAttribute("form",f);m.addAttribute("editing",false);return "form";
    }
    @PostMapping("/plans") String create(@Valid @ModelAttribute("form") PlanForm form,BindingResult errors,
            HttpSession session,Model m,RedirectAttributes redirect,HttpServletResponse response){
        if(errors.hasErrors()){response.setStatus(422);m.addAttribute("editing",false);return "form";}
        PlanService.Created c=service.create(form);grant(session,c.plan().getId());
        redirect.addFlashAttribute("newEditKey",c.editKey());
        return "redirect:/p/"+c.plan().getId();
    }
    @GetMapping("/p/{id}") String view(@PathVariable String id,HttpSession session,Model m){
        m.addAttribute("plan",service.get(id));m.addAttribute("owner",owns(session,id));return "plan";
    }
    @GetMapping("/p/{id}/share") String share(@PathVariable String id,Model m){
        m.addAttribute("plan",service.get(id));m.addAttribute("owner",false);return "plan";
    }
    @GetMapping("/p/{id}/edit") String edit(@PathVariable String id,HttpSession session,Model m){
        Plan p=service.get(id);m.addAttribute("plan",p);
        if(!owns(session,id))return "unlock";
        m.addAttribute("form",PlanForm.from(p));m.addAttribute("editing",true);return "form";
    }
    @PostMapping("/p/{id}/unlock") String unlock(@PathVariable String id,@RequestParam String editKey,
            HttpSession session,Model m,HttpServletResponse response){
        if(!service.verifyKey(id,editKey)){
            response.setStatus(403);m.addAttribute("plan",service.get(id));m.addAttribute("wrongKey",true);return "unlock";
        }
        grant(session,id);return "redirect:/p/"+id+"/edit";
    }
    @PostMapping("/p/{id}") String update(@PathVariable String id,@Valid @ModelAttribute("form") PlanForm form,
            BindingResult errors,HttpSession session,Model m,HttpServletResponse response,RedirectAttributes redirect){
        if(!owns(session,id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        m.addAttribute("plan",service.get(id));m.addAttribute("editing",true);
        if(errors.hasErrors()){response.setStatus(422);return "form";}
        try{service.update(id,form);}
        catch(PlanService.StalePlanException | OptimisticLockingFailureException e){
            response.setStatus(409);m.addAttribute("conflict",true);return "form";
        }
        redirect.addFlashAttribute("saved",true);return "redirect:/p/"+id;
    }
    @GetMapping("/healthz") @ResponseBody String health(){return "ok";}

    @SuppressWarnings("unchecked") private boolean owns(HttpSession session,String id){
        Object grants=session.getAttribute("ownedPlans");return grants instanceof Set<?> && ((Set<String>)grants).contains(id);
    }
    @SuppressWarnings("unchecked") private void grant(HttpSession session,String id){
        Set<String> grants=new LinkedHashSet<>();Object old=session.getAttribute("ownedPlans");
        if(old instanceof Set<?>) grants.addAll((Set<String>)old);
        grants.add(id);if(grants.size()>50)grants.remove(grants.iterator().next());
        session.setAttribute("ownedPlans",grants);
    }
}
