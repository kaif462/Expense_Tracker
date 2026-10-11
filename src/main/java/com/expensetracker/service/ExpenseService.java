package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private  ExpenseRepository expenseRepository;
    private  UserRepository userRepository;
    

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
		super();
		this.expenseRepository = expenseRepository;
		this.userRepository = userRepository;
	}

	private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = getCurrentUser();

        Expense expense = new Expense();
                expense.setTitle(request.getTitle());
               expense.setAmount(request.getAmount());
              expense.setCategory(request.getCategory());
               expense.setDate(request.getDate());
                expense.setNotes(request.getNotes());
                expense.setUser(user);
                

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    public List<ExpenseResponse> getExpenses(String category, LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        List<Expense> expenses = expenseRepository.findByUserWithFilters(user, category, startDate, endDate);
        return expenses.stream().map(this::toResponse).toList();
    }

    public ExpenseResponse getExpenseById(Long id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return toResponse(expense);
    }

    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDate(request.getDate());
        expense.setNotes(request.getNotes());

        Expense updated = expenseRepository.save(expense);
        return toResponse(updated);
    }

    public void deleteExpense(Long id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense expense) {
    	ExpenseResponse er=new ExpenseResponse();
    	er.setAmount(expense.getAmount());
    	er.setCategory(expense.getCategory());
    	er.setDate(expense.getDate());
    	er.setId(expense.getId());
    	er.setTitle(expense.getTitle());
    	er.setNotes(expense.getNotes());
    	
        return er;
    }
}
