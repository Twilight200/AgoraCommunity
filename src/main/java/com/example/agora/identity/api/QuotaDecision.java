package com.example.agora.identity.api;

public record QuotaDecision(
        boolean allowed,
        String reasonCode,
        int remaining
) {
    public static QuotaDecision allow(int remaining){
        return new QuotaDecision(true, null, remaining);
    }

    public static QuotaDecision deny(String reasonCode){
        return new QuotaDecision(false, reasonCode, 0);
    }
}
