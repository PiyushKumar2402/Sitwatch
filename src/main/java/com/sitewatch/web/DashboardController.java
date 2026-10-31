package com.sitewatch.web;

import com.sitewatch.dto.SiteRow;
import com.sitewatch.model.AppUser;
import com.sitewatch.model.SiteStatus;
import com.sitewatch.repo.IncidentRepository;
import com.sitewatch.service.StatsService;
import com.sitewatch.service.UserService;
import com.sitewatch.service.WebsiteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    private final UserService users;
    private final WebsiteService websites;
    private final StatsService stats;
    private final IncidentRepository incidents;

    public DashboardController(UserService users, WebsiteService websites, StatsService stats,
                               IncidentRepository incidents) {
        this.users = users;
        this.websites = websites;
        this.stats = stats;
        this.incidents = incidents;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        AppUser user = users.require(principal.getName());
        List<SiteRow> rows = websites.list(user).stream().map(stats::row).toList();

        long up = rows.stream().filter(r -> r.getSite().getLastStatus() == SiteStatus.UP).count();
        long down = rows.stream().filter(r -> r.getSite().getLastStatus() == SiteStatus.DOWN).count();
        double avg = rows.stream().filter(r -> r.getSite().getLastStatus() == SiteStatus.UP
                        && r.getSite().getLastResponseMs() != null)
                .mapToLong(r -> r.getSite().getLastResponseMs()).average().orElse(-1);

        model.addAttribute("rows", rows);
        model.addAttribute("total", rows.size());
        model.addAttribute("upCount", up);
        model.addAttribute("downCount", down);
        model.addAttribute("avgText", avg < 0 ? "—" : Math.round(avg) + " ms");
        model.addAttribute("incidents", incidents.findTop10ByWebsiteOwnerOrderByStartedAtDesc(user));
        return "dashboard";
    }
}
