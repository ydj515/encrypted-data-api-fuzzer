package com.example.gateway.crypto;

public interface CryptoModule {

    String encrypt(String key, String plainJson);

    String decrypt(String key, String encrypted);
}
