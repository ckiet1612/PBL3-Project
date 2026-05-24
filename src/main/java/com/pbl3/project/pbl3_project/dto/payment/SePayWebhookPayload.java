package com.pbl3.project.pbl3_project.dto.payment;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SePayWebhookPayload {
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("PBL(\\d+)", Pattern.CASE_INSENSITIVE);
    private Long id;
    private String gateway;
    @JsonAlias("transaction_date")
    private String transactionDate;
    @JsonAlias("account_number")
    private String accountNumber;
    @JsonAlias("sub_account")
    private String subAccount;
    private String code;
    private String content;
    @JsonAlias("transfer_type")
    private String transferType;
    private String description;
    @JsonAlias("transfer_amount")
    private BigDecimal transferAmount;
    private BigDecimal accumulated;
    @JsonAlias("reference_code")
    private String referenceCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getSubAccount() { return subAccount; }
    public void setSubAccount(String subAccount) { this.subAccount = subAccount; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getTransferAmount() { return transferAmount; }
    public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }

    public BigDecimal getAccumulated() { return accumulated; }
    public void setAccumulated(BigDecimal accumulated) { this.accumulated = accumulated; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public Long orderCode() {
        String rawCode = code != null && !code.isBlank() ? code : content;
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        Matcher matcher = PAYMENT_CODE_PATTERN.matcher(rawCode);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        String digits = rawCode.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return null;
        }
        return Long.parseLong(digits);
    }

    public String paymentLinkId() {
        return id == null ? null : String.valueOf(id);
    }

    public BigDecimal amount() {
        return transferAmount == null ? BigDecimal.ZERO : transferAmount;
    }

    public boolean isIncoming() {
        return transferType != null && "in".equalsIgnoreCase(transferType.trim());
    }
}
