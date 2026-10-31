package com.sitewatch.web;

import com.sitewatch.dto.Point;
import com.sitewatch.model.CheckResult;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.service.UserService;
import com.sitewatch.service.WebsiteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class ApiController {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UserService users;
    private final WebsiteService websites;
    private final CheckResultRepository checks;

    public ApiController(UserService users, WebsiteService websites, CheckResultRepository checks) {
        this.users = users;
        this.websites = websites;
        this.checks = checks;
    }

    /** Last 60 checks, oldest first. Feeds the response-time chart. */
    @GetMapping("/api/websites/{id}/history")
    public List<Point> history(@PathVariable Long id, Principal principal) {
        Website w = websites.get(id, users.require(principal.getName()));
        List<CheckResult> list = new ArrayList<>(checks.findTop60ByWebsiteOrderByCheckedAtDesc(w));
        Collections.reverse(list);
        return list.stream().map(c -> new Point(c.getCheckedAt().format(HM), c.getResponseTimeMs(), c.isUp())).toList();
    }
}
