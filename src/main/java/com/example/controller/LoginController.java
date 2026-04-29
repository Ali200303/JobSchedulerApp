package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password123";

    @GetMapping("/login")
    public String showLoginPage() {

        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {

        if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {

            session.setAttribute("loggedInUser", username);
            return "redirect:/jobs";
        } else {

            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();
        return "redirect:/login";
    }
}