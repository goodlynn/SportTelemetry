package com.sports.telemetry;

public class SessionResult {
    private final String athleteName;
    private final String duration;
    private final int avgHeartRate;
    private final int maxHeartRate;
    private final String finalZone;
    private final String hrPoints; // Точки графика для воссоздания истории

    public SessionResult(String athleteName, String duration, int avgHeartRate, int maxHeartRate, String finalZone, String hrPoints) {
        this.athleteName = athleteName;
        this.duration = duration;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.finalZone = finalZone;
        this.hrPoints = hrPoints;
    }

    public String getAthleteName() { return athleteName; }
    public String getDuration() { return duration; }
    public int getAvgHeartRate() { return avgHeartRate; }
    public int getMaxHeartRate() { return maxHeartRate; }
    public String getFinalZone() { return finalZone; }
    public String getHrPoints() { return hrPoints; }
}