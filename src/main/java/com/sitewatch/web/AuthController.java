package com.sitewatch.web;

import com.sitewatch.dto.RegisterForm;
import com.sitewatch.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result) {
        if (result.hasErrors()) return "register";
        try {
            users.register(form);
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "taken", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
