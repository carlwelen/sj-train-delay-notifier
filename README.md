# 🚆 SJ Train Delay Notifier

Get push notifications on your phone whenever an SJ train from **Enköping C → Stockholm C** is delayed, late, or cancelled.

## Overview

This project monitors real-time SJ train departures from Enköping C heading to Stockholm C using the [Trafikverket Open API](https://api.trafikinfo.trafikverket.se/). When a delay or cancellation is detected, a push notification is sent directly to your phone.

## How It Works

```
Trafikverket API  →  Monitor Script  →  Push Notification  →  📱 Your Phone
(real-time data)     (polls every 5m)    (ntfy / Pushover)
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

### Notification Services

| Service | Cost | Notes |
|---------|------|-------|
| [ntfy.sh](https://ntfy.sh) | Free | Open source, no account needed, Android & iOS apps |
| [Pushover](https://pushover.net) | $5 one-time | Reliable, polished native apps |
| [Pushbullet](https://pushbullet.com) | Free (100/mo) | Browser extension + mobile app |

### Hosting Options

| Option | Cost | Notes |
|--------|------|-------|
| Raspberry Pi | ~Free | Low power, always on |
| Free cloud tier (Oracle Cloud, Fly.io) | Free | No hardware needed |
| GitHub Actions (cron) | Free (public repos) | Runs on a schedule |
| VPS (Hetzner, DigitalOcean) | ~$4/mo | Full control |

## Getting Started

### Prerequisites

1. **Trafikverket API Key** — Register at [api.trafikinfo.trafikverket.se](https://api.trafikinfo.trafikverket.se/)
2. **Notification service** — Install [ntfy](https://ntfy.sh) (recommended) on your phone and pick a secret topic name
3. **Python 3.8+** with `requests` installed

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

## Status

🚧 **Work in progress** — code coming soon.