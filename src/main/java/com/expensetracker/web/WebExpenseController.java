package com.expensetracker.web;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

/**
 * Thymeleaf pages for managing expenses. Delegates all data access to {@link ExpenseService},
 * so each user only ever sees their own expenses (same rule as the REST API).
 */
@Controller
@RequiredArgsConstructor
public class WebExpenseController {

    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Education", "Other");
    @Autowired
    private  ExpenseService expenseService;

    /** Currency symbol shown in the UI. Override with app.currency-symbol in application.properties. */
    @Value("${app.currency-symbol:$}")
    private String currencySymbol;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Trim text fields; blank input becomes null so @NotBlank and optional filters behave.
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @ModelAttribute("username")
    public String username(Principal principal) {
        return principal.getName();
    }

    @ModelAttribute("currency")
    public String currency() {
        return currencySymbol;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/expenses";
    }

    // ---------------------------------------------------------------- list

    @GetMapping("/expenses")
    public String list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        String categoryFilter = (category == null || category.isBlank()) ? null : category.trim();
        boolean filtered = categoryFilter != null || startDate != null || endDate != null;

        List<ExpenseResponse> expenses = expenseService.getExpenses(categoryFilter, startDate, endDate);
        List<ExpenseResponse> everything = filtered ? expenseService.getExpenses(null, null, null) : expenses;

        BigDecimal total = expenses.stream()
                .map(ExpenseResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("expenses", expenses);
        model.addAttribute("total", total);
        model.addAttribute("summaries", summarise(expenses, total));
        model.addAttribute("categories", distinctCategories(everything));
        model.addAttribute("selectedCategory", categoryFilter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("filtered", filtered);
        return "expenses";
    }

    // ---------------------------------------------------------------- create

    @GetMapping("/expenses/new")
    public String newForm(Model model) {
        ExpenseRequest request = new ExpenseRequest();
        request.setDate(LocalDate.now());
        model.addAttribute("expenseRequest", request);
        prepareForm(model, "/expenses", "Add expense", "Add expense");
        return "expense-form";
    }

    @PostMapping("/expenses")
    public String create(
            @Valid @ModelAttribute("expenseRequest") ExpenseRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            prepareForm(model, "/expenses", "Add expense", "Add expense");
            return "expense-form";
        }
        expenseService.createExpense(request);
        redirectAttributes.addFlashAttribute("success", "Expense added.");
        return "redirect:/expenses";
    }

    // ---------------------------------------------------------------- edit

    @GetMapping("/expenses/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ExpenseResponse existing = expenseService.getExpenseById(id);

        ExpenseRequest request = new ExpenseRequest();
        request.setTitle(existing.getTitle());
        request.setAmount(existing.getAmount());
        request.setCategory(existing.getCategory());
        request.setDate(existing.getDate());
        request.setNotes(existing.getNotes());

        model.addAttribute("expenseRequest", request);
        prepareForm(model, "/expenses/" + id, "Edit expense", "Save changes");
        return "expense-form";
    }

    @PostMapping("/expenses/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("expenseRequest") ExpenseRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            prepareForm(model, "/expenses/" + id, "Edit expense", "Save changes");
            return "expense-form";
        }
        expenseService.updateExpense(id, request);
        redirectAttributes.addFlashAttribute("success", "Changes saved.");
        return "redirect:/expenses";
    }

    // ---------------------------------------------------------------- delete

    @PostMapping("/expenses/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        expenseService.deleteExpense(id);
        redirectAttributes.addFlashAttribute("success", "Expense deleted.");
        return "redirect:/expenses";
    }

    // ---------------------------------------------------------------- helpers

    private void prepareForm(Model model, String action, String pageTitle, String submitLabel) {
        Set<String> suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        suggestions.addAll(DEFAULT_CATEGORIES);
        expenseService.getExpenses(null, null, null).forEach(e -> suggestions.add(e.getCategory()));

        model.addAttribute("formAction", action);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("categorySuggestions", List.copyOf(suggestions));
    }

    private List<String> distinctCategories(List<ExpenseResponse> expenses) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        expenses.forEach(e -> names.add(e.getCategory()));
        return List.copyOf(names);
    }

    private List<CategorySummary> summarise(List<ExpenseResponse> expenses, BigDecimal total) {
        // Case-insensitive so "Food" and "food" land in the same slice.
        Map<String, BigDecimal> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ExpenseResponse e : expenses) {
            byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }

        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> new CategorySummary(
                        entry.getKey(),
                        entry.getValue(),
                        total.signum() == 0
                                ? 0
                                : entry.getValue().multiply(BigDecimal.valueOf(100))
                                        .divide(total, 1, RoundingMode.HALF_UP).doubleValue()))
                .toList();
    }
}
