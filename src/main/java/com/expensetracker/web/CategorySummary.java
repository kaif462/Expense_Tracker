package com.expensetracker.web;

import java.math.BigDecimal;

/** One slice of the "where it went" breakdown shown on the expenses page. */
public record CategorySummary(String name, BigDecimal total, double percent) {
}
