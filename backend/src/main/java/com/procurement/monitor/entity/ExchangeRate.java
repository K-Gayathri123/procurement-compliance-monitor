package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id", nullable = false)
    private Long rateId;

    @Column(name = "from_currency", nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false)
    private String toCurrency;

    @Column(name = "rate_date", nullable = false)
    private String rateDate;

    @Column(name = "conversion_rate", nullable = false)
    private String conversionRate;

    @Column(name = "rate_source", nullable = false)
    private String rateSource;

    public ExchangeRate() {}

    public Long getRateId() {
        return rateId;
    }

    public void setRateId(Long value) {
        this.rateId = value;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String value) {
        this.fromCurrency = value;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String value) {
        this.toCurrency = value;
    }

    public String getRateDate() {
        return rateDate;
    }

    public void setRateDate(String value) {
        this.rateDate = value;
    }

    public String getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(String value) {
        this.conversionRate = value;
    }

    public String getRateSource() {
        return rateSource;
    }

    public void setRateSource(String value) {
        this.rateSource = value;
    }

}
