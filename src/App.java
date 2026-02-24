import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for the SJ Train Delay Notifier.
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>{@code TRAFIKVERKET_API_KEY} — your Trafikverket Open Data API key</li>
 *   <li>{@code NTFY_TOPIC} — full ntfy topic URL, e.g. {@code https://ntfy.sh/my-sj-alerts}
 *       (defaults to {@code https://ntfy.sh/sj-train-delays} if not set)</li>
 * </ul>
 *
 * <p>The monitor polls the Trafikverket API every {@value #POLL_INTERVAL_MINUTES} minutes and
 * sends a push notification whenever an SJ train from Enköping C to Stockholm C is delayed
 * or cancelled.
 */
public class App {

    private static final int POLL_INTERVAL_MINUTES = 5;
    private static final String DEFAULT_NTFY_URL = "https://ntfy.sh/sj-train-delays";

    public static void main(String[] args) {
        String apiKey = System.getenv("TRAFIKVERKET_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Error: TRAFIKVERKET_API_KEY environment variable is not set.");
            System.err.println("Register for a free API key at https://api.trafikinfo.trafikverket.se/");
            System.exit(1);
        }

        String ntfyUrl = System.getenv("NTFY_TOPIC");
        if (ntfyUrl == null || ntfyUrl.isBlank()) {
            ntfyUrl = DEFAULT_NTFY_URL;
        }

        TrafikverketClient client = new TrafikverketClient(apiKey);
        NotificationService notifier = new NotificationService(ntfyUrl);
        DelayDetector detector = new DelayDetector();

        // Track which (train + status) combinations have already been notified to avoid duplicates.
        Set<String> notified = new HashSet<>();

        System.out.println("SJ Train Delay Notifier started.");
        System.out.println("Route : Enköping C → Stockholm C");
        System.out.println("Poll  : every " + POLL_INTERVAL_MINUTES + " minutes");
        System.out.println("Notify: " + ntfyUrl);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> poll(client, notifier, detector, notified),
                0, POLL_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private static void poll(TrafikverketClient client, NotificationService notifier,
                              DelayDetector detector, Set<String> notified) {
        try {
            List<TrainAnnouncement> departures = client.fetchDepartures();
            System.out.println("Fetched " + departures.size() + " departure(s).");

            // Prune the notified set: keep only entries for trains still in the active window.
            // This prevents unbounded growth in long-running deployments.
            Set<String> activeIds = new HashSet<>();
            for (TrainAnnouncement t : departures) activeIds.add(t.getTrainId());
            notified.removeIf(key -> {
                String trainId = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
                return !activeIds.contains(trainId);
            });

            for (TrainAnnouncement train : departures) {
                if (detector.isCanceled(train)) {
                    String key = train.getTrainId() + ":canceled";
                    if (notified.add(key)) {
                        System.out.println("[CANCELLED] " + train);
                        notifier.sendCancellationNotification(train);
                    }
                } else if (detector.isDelayed(train)) {
                    long delayMin = detector.getDelayMinutes(train);
                    String key = train.getTrainId() + ":delayed:" + delayMin;
                    if (notified.add(key)) {
                        System.out.println("[DELAYED " + delayMin + " min] " + train);
                        notifier.sendDelayNotification(train, delayMin);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Polling error: " + e.getMessage());
        }
    }
}