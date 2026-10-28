package com.tripfactory.nomad.service;

/** Sends phone/browser notifications that arrive even when the app is closed. */
public interface PushService {

    /** @return true if the server has its push keys set up. */
    boolean isEnabled();

    String publicKey();

    /** Sends to every device the user signed up. Best-effort and never throws. */
    void sendToUser(Long userId, String title, String body, String link);
}
