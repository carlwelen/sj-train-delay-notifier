import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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

    public void sendDelayNotification(TrainAnnouncement train, long delayMinutes) throws Exception {
        String title = "\uD83D\uDE86 SJ Train Delayed";
        String body = String.format(
                "Train %s (Enköping C \u2192 Stockholm C) is delayed by %d minute(s).%n"
                        + "Scheduled: %s  |  Expected: %s",
                train.getTrainId(),
                delayMinutes,
                train.getAdvertisedTime().toLocalTime(),
                train.getEstimatedTime().toLocalTime());
        sendNotification(title, body);
    }

    public void sendCancellationNotification(TrainAnnouncement train) throws Exception {
        String title = "\uD83D\uDE86 SJ Train Canceled";
        String body = String.format(
                "Train %s (Enköping C \u2192 Stockholm C) scheduled at %s has been cancelled.",
                train.getTrainId(),
                train.getAdvertisedTime().toLocalTime());
        if (train.getDeviation() != null && !train.getDeviation().isBlank()) {
            body += " Reason: " + train.getDeviation();
        }
        sendNotification(title, body);
    }
}
