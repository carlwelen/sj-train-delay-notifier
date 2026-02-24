import java.time.OffsetDateTime;

/**
 * Represents a single train departure announcement from the Trafikverket API.
 */
public class TrainAnnouncement {

    private final String trainId;
    private final OffsetDateTime advertisedTime;
    private final OffsetDateTime estimatedTime; // null if on time
    private final String toLocation;
    private final boolean canceled;
    private final String deviation;

    public TrainAnnouncement(
            String trainId,
            OffsetDateTime advertisedTime,
            OffsetDateTime estimatedTime,
            String toLocation,
            boolean canceled,
            String deviation) {
        this.trainId = trainId;
        this.advertisedTime = advertisedTime;
        this.estimatedTime = estimatedTime;
        this.toLocation = toLocation;
        this.canceled = canceled;
        this.deviation = deviation;
    }

    public String getTrainId() { return trainId; }
    public OffsetDateTime getAdvertisedTime() { return advertisedTime; }
    public OffsetDateTime getEstimatedTime() { return estimatedTime; }
    public String getToLocation() { return toLocation; }
    public boolean isCanceled() { return canceled; }
    public String getDeviation() { return deviation; }

    @Override
    public String toString() {
        return String.format("Train %s [%s -> %s | canceled=%b | deviation=%s]",
                trainId, advertisedTime, estimatedTime, canceled, deviation);
    }
}