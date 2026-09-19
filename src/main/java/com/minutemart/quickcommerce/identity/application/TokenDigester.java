package com.minutemart.quickcommerce.identity.application;

public interface TokenDigester {

    String digest(String rawValue);
}
