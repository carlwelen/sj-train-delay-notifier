import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Sends push notifications via ntfy (https://ntfy.sh).
 *
 * Configure the target topic with the NTFY_TOPIC environment variable,
 * e.g. "https://ntfy.sh/my-sj-alerts".
 */
public class NotificationService {

    private final String ntfyUrl;
    private final HttpClient httpClient;

    public NotificationService(String ntfyUrl) {
        this.ntfyUrl = ntfyUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends a push notification with the given title and body.
     */
    public void sendNotification(String title, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ntfyUrl))
                .header("Title", title)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("Failed to send notification: HTTP " + response.statusCode());
        }
    }

    /**
     * Sends a summary notification for a categorized delay report.
     */
    public void sendSummaryNotification(DelayDetector.DelayReport report,
                                         DelayDetector detector) throws Exception {
        StringBuilder body = new StringBuilder();

        if (!report.getSeverelyDelayed().isEmpty()) {
            body.append("🔴 SEVERELY DELAYED (60+ min):\n");
            for (TrainAnnouncement t : report.getSeverelyDelayed()) {
                long delay = detector.getDelayMinutes(t);
                body.append(String.format("  Train %s | Scheduled: %s | Expected: %s | +%d min\n",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime(),
                        t.getEstimatedTime().toLocalTime(),
                        delay));
            }
            body.append("\n");
        }

        if (!report.getModeratelyDelayed().isEmpty()) {
            body.append("🟡 DELAYED (20–59 min):\n");
            for (TrainAnnouncement t : report.getModeratelyDelayed()) {
                long delay = detector.getDelayMinutes(t);
                body.append(String.format("  Train %s | Scheduled: %s | Expected: %s | +%d min\n",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime(),
                        t.getEstimatedTime().toLocalTime(),
                        delay));
            }
            body.append("\n");
        }

        if (!report.getCancelled().isEmpty()) {
            body.append("⛔ CANCELLED:\n");
            for (TrainAnnouncement t : report.getCancelled()) {
                body.append(String.format("  Train %s | Scheduled: %s",
                        t.getTrainId(),
                        t.getAdvertisedTime().toLocalTime()));
                if (t.getDeviation() != null && !t.getDeviation().isBlank()) {
                    body.append(" | Reason: ").append(t.getDeviation());
                }
                body.append("\n");
            }
        }

        String title = "\uD83D\uDE86 SJ Delay Report — Enköping C → Stockholm C";
        sendNotification(title, body.toString().trim());
    }
}
