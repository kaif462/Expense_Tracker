package com.expensetracker.web;

import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Login and registration pages. Login itself (POST /login) is handled by Spring Security's form login.
 */
@Controller
@RequiredArgsConstructor
public class WebAuthController {
     @Autowired
    private  UserRepository userRepository;
     @Autowired
    private  PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage(Principal principal) {
        return principal != null ? "redirect:/expenses" : "login";
    }

    @GetMapping("/register")
    public String registerPage(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/expenses";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {
        if (!result.hasFieldErrors("username") && userRepository.existsByUsername(request.getUsername())) {
            result.rejectValue("username", "username.taken", "That username is already taken");
        }

        if (result.hasErrors()) {
            return "register";
        }

        User user =new User();
                user.setUsername(request.getUsername());
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setEmail(request.getEmail());
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Account created. Sign in to start tracking.");
        return "redirect:/login";
    }
}
