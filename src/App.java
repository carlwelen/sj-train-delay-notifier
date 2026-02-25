import java.util.List;

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
 * <p>When launched, the application fetches current SJ departures from Enköping C to
 * Stockholm C and prints a categorized delay report:
 * <ul>
 *   <li>🔴 Severely delayed — 60+ minutes late</li>
 *   <li>🟡 Moderately delayed — 20–59 minutes late</li>
 *   <li>⛔ Cancelled</li>
 * </ul>
 * A summary push notification is sent via ntfy if any issues are found.
 */
public class App {

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

        System.out.println("🚆 SJ Train Delay Report");
        System.out.println("   Route: Enköping C → Stockholm C");
        System.out.println();

        try {
            List<TrainAnnouncement> departures = client.fetchDepartures();
            System.out.println("Fetched " + departures.size() + " departure(s).\n");

            DelayDetector.DelayReport report = detector.categorize(departures);

            if (report.isEmpty()) {
                System.out.println("✅ No significant delays or cancellations found.");
                return;
            }

            printReport(report, detector);

            try {
                notifier.sendSummaryNotification(report, detector);
                System.out.println("\n📱 Summary notification sent via ntfy.");
            } catch (Exception e) {
                System.err.println("Failed to send notification: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error fetching departures: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printReport(DelayDetector.DelayReport report, DelayDetector detector) {
        if (!report.getSeverelyDelayed().isEmpty()) {
            System.out.println("🔴 SEVERELY DELAYED (60+ minutes):");
            for (TrainAnnouncement t : report.getSeverelyDelayed()) {
                long delay = detector.getDelayMinutes(t);
                System.out.printf("   Train %s | Scheduled: %s | Expected: %s | +%d min%n",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime(),
                        t.getEstimatedTime().toLocalTime(),
                        delay);
            }
            System.out.println();
        }

        if (!report.getModeratelyDelayed().isEmpty()) {
            System.out.println("🟡 DELAYED (20–59 minutes):");
            for (TrainAnnouncement t : report.getModeratelyDelayed()) {
                long delay = detector.getDelayMinutes(t);
                System.out.printf("   Train %s | Scheduled: %s | Expected: %s | +%d min%n",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime(),
                        t.getEstimatedTime().toLocalTime(),
                        delay);
            }
            System.out.println();
        }

        if (!report.getCancelled().isEmpty()) {
            System.out.println("⛔ CANCELLED:");
            for (TrainAnnouncement t : report.getCancelled()) {
                System.out.printf("   Train %s | Scheduled: %s",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime());
                if (t.getDeviation() != null && !t.getDeviation().isBlank()) {
                    System.out.print(" | Reason: " + t.getDeviation());
                }
                System.out.println();
            }
            System.out.println();
        }
    }
}