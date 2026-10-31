package com.sitewatch.web;

import com.sitewatch.dto.WebsiteForm;
import com.sitewatch.model.AppUser;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.repo.IncidentRepository;
import com.sitewatch.service.ReportService;
import com.sitewatch.service.StatsService;
import com.sitewatch.service.UserService;
import com.sitewatch.service.WebsiteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class WebsiteController {

    private final UserService users;
    private final WebsiteService websites;
    private final StatsService stats;
    private final ReportService reports;
    private final CheckResultRepository checks;
    private final IncidentRepository incidents;

    public WebsiteController(UserService users, WebsiteService websites, StatsService stats, ReportService reports,
                             CheckResultRepository checks, IncidentRepository incidents) {
        this.users = users;
        this.websites = websites;
        this.stats = stats;
        this.reports = reports;
        this.checks = checks;
        this.incidents = incidents;
    }

    @PostMapping("/websites")
    public String add(@Valid @ModelAttribute("form") WebsiteForm form, BindingResult result,
                      Principal principal, RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/dashboard";
        }
        try {
            Website w = websites.add(users.require(principal.getName()), form);
            flash.addFlashAttribute("success", w.getName() + " is now being monitored. The first check runs within seconds.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/websites/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        AppUser user = users.require(principal.getName());
        Website w = websites.get(id, user);
        WebsiteForm form = new WebsiteForm();
        form.setName(w.getName());
        form.setUrl(w.getUrl());
        form.setIntervalSeconds(w.getIntervalSeconds());

        model.addAttribute("site", w);
        model.addAttribute("stats", stats.compute(w));
        model.addAttribute("daily", reports.lastDays(w, 7));
        model.addAttribute("incidents", incidents.findTop20ByWebsiteOrderByStartedAtDesc(w));
        model.addAttribute("recent", checks.findTop30ByWebsiteOrderByCheckedAtDesc(w));
        model.addAttribute("form", form);
        return "website";
    }

    @PostMapping("/websites/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") WebsiteForm form, BindingResult result,
                       Principal principal, RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
        } else {
            try {
                websites.update(id, users.require(principal.getName()), form);
                flash.addFlashAttribute("success", "Changes saved.");
            } catch (IllegalArgumentException e) {
                flash.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/websites/" + id;
    }

    @PostMapping("/websites/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes flash,
                         @RequestParam(defaultValue = "dashboard") String from) {
        websites.toggle(id, users.require(principal.getName()));
        flash.addFlashAttribute("success", "Monitoring updated.");
        return "details".equals(from) ? "redirect:/websites/" + id : "redirect:/dashboard";
    }

    @PostMapping("/websites/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        websites.delete(id, users.require(principal.getName()));
        flash.addFlashAttribute("success", "Website deleted along with its history.");
        return "redirect:/dashboard";
    }
}
