package com.soen345.project.auth;

public interface AuthCallback {
    void onSuccess(AuthSession session);

    void onError(String errorMessage);
}
