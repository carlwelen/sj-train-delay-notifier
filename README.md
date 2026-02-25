# 🚆 SJ Train Delay Notifier

Run once to get a categorized delay report for SJ trains from **Enköping C → Stockholm C**. No need to keep it running — just launch it when you want to check.

## Overview

This application fetches real-time SJ train departures from Enköping C to Stockholm C using the [Trafikverket Open API](https://api.trafikinfo.trafikverket.se/) and prints a categorized delay report:

- **Severely delayed** — trains more than **60 minutes** late
- **Delayed** — trains more than **20 minutes** late (up to 59 min)
- **Cancelled** — trains that have been cancelled

A summary push notification is also sent to your phone via [ntfy](https://ntfy.sh).

## How It Works

```
Launch app  →  Fetch from Trafikverket API  →  Print categorized report  →  Send notification  →  Exit
```

### Data Source

- **API:** [Trafikverket Open Data API](https://api.trafikinfo.trafikverket.se/) (free, requires registration)
- **Object:** `TrainAnnouncement` — provides real-time train departure/arrival info
- **Filters used:**
  - `LocationSignature = "Ek"` (Enköping C)
  - `ToLocation` includes `"Cst"` (Stockholm Central)
  - `InformationOwner = "SJ"` (only SJ-operated trains)
- **Detection:**
  - **Delay:** `EstimatedTimeAtLocation` differs from `AdvertisedTimeAtLocation` by 20+ minutes
  - **Cancellation:** `Canceled = true`

## Getting Started

### Prerequisites

1. **Trafikverket API Key** — Register at [data.trafikverket.se](https://data.trafikverket.se/) to obtain a free API key
2. **Notification service** — Install [ntfy](https://ntfy.sh)

### Running (Unix/Linux/macOS)

```bash
# Set your Trafikverket API key
export TRAFIKVERKET_API_KEY="your-api-key-here"

# Optional: set a custom ntfy topic (defaults to https://ntfy.sh/sj-train-delays)
export NTFY_TOPIC="https://ntfy.sh/my-sj-alerts"

# Compile and run
javac -d out src/*.java
java -cp out App
```

> **Note:** Make sure you have a JDK (version 11 or later) installed. You can verify with `java -version` and `javac -version`.

### Station Codes

| Station | Code |
|---------|------|
| Enköping C | `Ek` |
| Stockholm C | `Cst` |

## API Reference

### Example Trafikverket Query

```xml
<REQUEST>
  <LOGIN authenticationkey="YOUR_API_KEY" />
  <QUERY objecttype="TrainAnnouncement" schemaversion="1.9" orderby="AdvertisedTimeAtLocation">
    <FILTER>
      <AND>
        <EQ name="LocationSignature" value="Ek" />
        <EQ name="ActivityType" value="Avgang" />
        <EQ name="InformationOwner" value="SJ" />
        <GT name="AdvertisedTimeAtLocation" value="$dateadd(-01:00:00)" />
        <LT name="AdvertisedTimeAtLocation" value="$dateadd(12:00:00)" />
      </AND>
    </FILTER>
    <INCLUDE>AdvertisedTrainIdent</INCLUDE>
    <INCLUDE>AdvertisedTimeAtLocation</INCLUDE>
    <INCLUDE>EstimatedTimeAtLocation</INCLUDE>
    <INCLUDE>ToLocation</INCLUDE>
    <INCLUDE>Canceled</INCLUDE>
    <INCLUDE>Deviation</INCLUDE>
  </QUERY>
</REQUEST>
```

**Endpoint:** `POST https://api.trafikinfo.trafikverket.se/v2/data.json`

### Useful Links

- [Trafikverket API Documentation](https://api.trafikinfo.trafikverket.se/)
- [Trafiklab Developer Portal](https://www.trafiklab.se/)
- [ntfy Documentation](https://docs.ntfy.sh/)

## License

MIT
