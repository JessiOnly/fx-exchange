package com.fx.api.model;

import java.time.LocalDate;

/** Row of Week-1's fx_rate table. Records: immutable, concise — the "newer features" payoff. */
public record Rate(int id, String baseCode, String quoteCode, double rate, LocalDate rateDate) {
    public String pair() { return baseCode + "/" + quoteCode; }
}
