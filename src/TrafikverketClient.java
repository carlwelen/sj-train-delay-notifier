import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches real-time SJ train departure announcements from the Trafikverket Open Data API.
 *
 * Monitors departures from Enköping C (Ek) to Stockholm C (Cst).
 */
public class TrafikverketClient {

    private static final String API_URL = "https://api.trafikinfo.trafikverket.se/v2/data.json";

    private static final Pattern TRAIN_ID  = Pattern.compile("\"AdvertisedTrainIdent\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ADV_TIME  = Pattern.compile("\"AdvertisedTimeAtLocation\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EST_TIME  = Pattern.compile("\"EstimatedTimeAtLocation\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CANCELED  = Pattern.compile("\"Canceled\"\\s*:\\s*(true|false)");
    private static final Pattern LOC_NAME  = Pattern.compile("\"LocationName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEVIATION = Pattern.compile("\"Description\"\\s*:\\s*\"([^\"]+)\"");

    private final String apiKey;
    private final HttpClient httpClient;

    public TrafikverketClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches upcoming SJ departures from Enköping C towards Stockholm C.
     */
    public List<TrainAnnouncement> fetchDepartures() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(buildQuery()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Trafikverket API error: HTTP " + response.statusCode()
                    + " — " + response.body());
        }

        return parseResponse(response.body());
    }

    private String buildQuery() {
        return "<REQUEST>"
                + "<LOGIN authenticationkey=\"" + apiKey + "\" />"
                + "<QUERY objecttype=\"TrainAnnouncement\" orderby=\"AdvertisedTimeAtLocation\">"
                + "<FILTER><AND>"
                + "<EQ name=\"LocationSignature\" value=\"Ek\" />"
                + "<EQ name=\"ActivityType\" value=\"Avgang\" />"
                + "<EQ name=\"InformationOwner\" value=\"SJ\" />"
                + "<IN name=\"ToLocation.LocationName\" value=\"Cst\" />"
                + "<GT name=\"AdvertisedTimeAtLocation\" value=\"$dateadd(-01:00:00)\" />"
                + "<LT name=\"AdvertisedTimeAtLocation\" value=\"$dateadd(12:00:00)\" />"
                + "</AND></FILTER>"
                + "<INCLUDE>AdvertisedTrainIdent</INCLUDE>"
                + "<INCLUDE>AdvertisedTimeAtLocation</INCLUDE>"
                + "<INCLUDE>EstimatedTimeAtLocation</INCLUDE>"
                + "<INCLUDE>ToLocation</INCLUDE>"
                + "<INCLUDE>Canceled</INCLUDE>"
                + "<INCLUDE>Deviation</INCLUDE>"
                + "</QUERY></REQUEST>";
    }

    // -------------------------------------------------------------------------
    // JSON parsing (no external dependencies)
    // -------------------------------------------------------------------------

    private List<TrainAnnouncement> parseResponse(String json) {
        List<TrainAnnouncement> list = new ArrayList<>();

        String marker = "\"TrainAnnouncement\":";
        int markerPos = json.indexOf(marker);
        if (markerPos < 0) return list;

        int arrayStart = json.indexOf('[', markerPos + marker.length());
        if (arrayStart < 0) return list;

        for (String obj : extractObjects(json, arrayStart)) {
            TrainAnnouncement announcement = parseAnnouncement(obj);
            if (announcement != null) list.add(announcement);
        }
        return list;
    }

    /** Splits a JSON array (starting at {@code arrayStart}) into individual top-level objects. */
    private List<String> extractObjects(String json, int arrayStart) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int objStart = -1;

        for (int i = arrayStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    objects.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return objects;
    }

    private TrainAnnouncement parseAnnouncement(String obj) {
        String trainId    = extract(TRAIN_ID, obj);
        String advTimeStr = extract(ADV_TIME, obj);
        if (trainId == null || advTimeStr == null) return null;

        String estTimeStr = extract(EST_TIME, obj);
        String canceledStr = extract(CANCELED, obj);
        String toLocation = extractToLocation(obj);
        String deviation  = extractDeviation(obj);

        OffsetDateTime advertisedTime = OffsetDateTime.parse(advTimeStr);
        OffsetDateTime estimatedTime  = estTimeStr != null ? OffsetDateTime.parse(estTimeStr) : null;
        boolean canceled = "true".equals(canceledStr);

        return new TrainAnnouncement(trainId, advertisedTime, estimatedTime, toLocation, canceled, deviation);
    }

    /** Extracts the first LocationName from the ToLocation array. */
    private String extractToLocation(String obj) {
        int start = obj.indexOf("\"ToLocation\":");
        if (start < 0) return null;
        int arrStart = obj.indexOf('[', start);
        int arrEnd   = obj.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return null;
        return extract(LOC_NAME, obj.substring(arrStart, arrEnd + 1));
    }

    /** Extracts the first deviation description, if any. */
    private String extractDeviation(String obj) {
        int start = obj.indexOf("\"Deviation\":");
        if (start < 0) return null;
        int arrStart = obj.indexOf('[', start);
        int arrEnd   = obj.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return null;
        return extract(DEVIATION, obj.substring(arrStart, arrEnd + 1));
    }

    private static String extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}