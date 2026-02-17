package com.soen345.project.auth;

public interface AuthCallback {
    void onSuccess(String userEmail);

    void onError(String errorMessage);
}
