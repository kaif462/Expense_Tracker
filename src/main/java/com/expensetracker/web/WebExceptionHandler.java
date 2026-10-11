package com.expensetracker.web;

import com.expensetracker.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.security.Principal;

/** Renders HTML error pages for the Thymeleaf controllers (the REST API keeps its JSON handler). */
@ControllerAdvice(basePackages = "com.expensetracker.web")
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Principal principal, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("username", principal != null ? principal.getName() : null);
        return "error";
    }
}
