package com.tripfactory.nomad.service;

/** Sends phone/browser notifications that arrive even when the app is closed. */
public interface PushService {

    /** @return true if the server has its push keys set up. */
    boolean isEnabled();

    String publicKey();

    /**
     * Queues a message for every device the user signed up. It is stored first and delivered by a background
     * worker, so it survives restarts and is retried if the push service is down. Never throws.
     */
    void sendToUser(Long userId, String title, String body, String link);

    /** Delivers the messages that are due. Called by a timer.
     *
     * @return how many were handled
     */
    int deliverDue();

    /** Messages still waiting to be delivered. */
    long pending();

    /** Messages that ran out of attempts. */
    long failed();

    /** Removes delivered and given-up messages older than a week. */
    void cleanUp();
}
