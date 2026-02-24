// Polling method to monitor both directions
public void poll() {
    // Call to fetch departures in one direction
    fetchDepartures();
    // Call to fetch departures in the reverse direction
    fetchDeparturesReverse();
}