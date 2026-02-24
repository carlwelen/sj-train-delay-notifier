import java.time.Duration;

/**
 * Detects whether a train announcement represents a delay or cancellation.
 */
public class DelayDetector {

    /** Minimum delay (in minutes) before a notification is sent. */
    static final int DELAY_THRESHOLD_MINUTES = 1;

    public boolean isDelayed(TrainAnnouncement train) {
        return getDelayMinutes(train) >= DELAY_THRESHOLD_MINUTES;
    }

    public boolean isCanceled(TrainAnnouncement train) {
        return train.isCanceled();
    }

    /**
     * Returns the delay in minutes (positive = late, negative = early).
     * Returns 0 if no estimated time is provided.
     */
    public long getDelayMinutes(TrainAnnouncement train) {
        if (train.getEstimatedTime() == null) return 0;
        return Duration.between(train.getAdvertisedTime(), train.getEstimatedTime()).toMinutes();
    }

    /** Returns true if the train requires a push notification. */
    public boolean requiresNotification(TrainAnnouncement train) {
        return isCanceled(train) || isDelayed(train);
    }
}