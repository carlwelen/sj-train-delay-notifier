# 🚆 SJ Train Delay Notifier

Get push notifications on your phone whenever an SJ train from **Enköping C → Stockholm C** is delayed, late, or cancelled.

## Overview

This project monitors real-time SJ train departures from Enköping C heading to Stockholm C using the [Trafikverket Open API](https://api.trafikinfo.trafikverket.se/). When a delay or cancellation is detected, a push notification is sent directly to your phone.

## How It Works

```
Trafikverket API  →  Monitor Script  →  Push Notification  →  Your Phone

```

### Data Source

- **API:** [Trafikverket Open Data API](https://api.trafikinfo.trafikverket.se/) (free, requires registration)
- **Object:** `TrainAnnouncement` — provides real-time train departure/arrival info
- **Filters used:**
  - `LocationSignature = "Ek"` (Enköping C)
  - `ToLocation` includes `"Cst"` (Stockholm Central)
  - `InformationOwner = "SJ"` (only SJ-operated trains)
- **Detection:**
  - **Delay:** `EstimatedTimeAtLocation` differs from `AdvertisedTimeAtLocation`
  - **Cancellation:** `Canceled = true`

## Getting Started

### Prerequisites

1. **Trafikverket API Key** — Register at [api.trafikinfo.trafikverket.se](https://api.trafikinfo.trafikverket.se/)
2. **Notification service** — Install [ntfy](https://ntfy.sh)

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
  <QUERY objecttype="TrainAnnouncement" orderby="AdvertisedTimeAtLocation">
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
