import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects whether a train announcement represents a delay or cancellation,
 * and categorizes delays into severity buckets.
 */
public class DelayDetector {

    /** Threshold for "moderately delayed" trains (20–59 minutes late). */
    static final int MODERATE_DELAY_MINUTES = 20;

    /** Threshold for "severely delayed" trains (60+ minutes late). */
    static final int SEVERE_DELAY_MINUTES = 60;

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

    /** Returns true if the train is delayed by 20 minutes or more. */
    public boolean isModeratelyDelayed(TrainAnnouncement train) {
        long delay = getDelayMinutes(train);
        return delay >= MODERATE_DELAY_MINUTES && delay < SEVERE_DELAY_MINUTES;
    }

    /** Returns true if the train is delayed by 60 minutes or more. */
    public boolean isSeverelyDelayed(TrainAnnouncement train) {
        return getDelayMinutes(train) >= SEVERE_DELAY_MINUTES;
    }

    /**
     * Categorizes a list of train announcements into three groups:
     * severely delayed (60+ min), moderately delayed (20–59 min), and cancelled.
     */
    public DelayReport categorize(List<TrainAnnouncement> trains) {
        List<TrainAnnouncement> severe = new ArrayList<>();
        List<TrainAnnouncement> moderate = new ArrayList<>();
        List<TrainAnnouncement> cancelled = new ArrayList<>();

        for (TrainAnnouncement train : trains) {
            if (isCanceled(train)) {
                cancelled.add(train);
            } else if (isSeverelyDelayed(train)) {
                severe.add(train);
            } else if (isModeratelyDelayed(train)) {
                moderate.add(train);
            }
        }

        return new DelayReport(severe, moderate, cancelled);
    }

    /** Holds the categorized results of a delay analysis. */
    public static class DelayReport {
        private final List<TrainAnnouncement> severelyDelayed;
        private final List<TrainAnnouncement> moderatelyDelayed;
        private final List<TrainAnnouncement> cancelled;

        public DelayReport(List<TrainAnnouncement> severelyDelayed,
                           List<TrainAnnouncement> moderatelyDelayed,
                           List<TrainAnnouncement> cancelled) {
            this.severelyDelayed = severelyDelayed;
            this.moderatelyDelayed = moderatelyDelayed;
            this.cancelled = cancelled;
        }

        public List<TrainAnnouncement> getSeverelyDelayed()  { return severelyDelayed; }
        public List<TrainAnnouncement> getModeratelyDelayed() { return moderatelyDelayed; }
        public List<TrainAnnouncement> getCancelled()         { return cancelled; }

        public boolean isEmpty() {
            return severelyDelayed.isEmpty() && moderatelyDelayed.isEmpty() && cancelled.isEmpty();
        }
    }
}