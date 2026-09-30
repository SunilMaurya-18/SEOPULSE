package com.seopulse.auth.service;

/**
 * Reports whether a password appears in known data breaches.
 */
public interface BreachedPasswordChecker {

    boolean isBreached(String password);
}
