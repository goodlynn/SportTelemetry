package com.sports.telemetry;

import javafx.beans.property.*;

public class TelemetryModel {
    private final IntegerProperty heartRate = new SimpleIntegerProperty(70);
    private final DoubleProperty workload = new SimpleDoubleProperty(0.0);
    private final DoubleProperty recovery = new SimpleDoubleProperty(1.0);
    private final StringProperty currentZone = new SimpleStringProperty("Покой");

    public int getHeartRate() { return heartRate.get(); }
    public IntegerProperty heartRateProperty() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate.set(heartRate); }

    public double getWorkload() { return workload.get(); }
    public DoubleProperty workloadProperty() { return workload; }
    public void setWorkload(double workload) { this.workload.set(workload); }

    public double getRecovery() { return recovery.get(); }
    public DoubleProperty recoveryProperty() { return recovery; }
    public void setRecovery(double recovery) { this.recovery.set(recovery); }

    public String getCurrentZone() { return currentZone.get(); }
    public StringProperty currentZoneProperty() { return currentZone; }
    public void setCurrentZone(String zone) { this.currentZone.set(zone); }

    public void updateZone() {
        int maxHR = 200; // 220 - возраст (условно 20 лет)
        int restingHR = 60;
        int hrReserve = maxHR - restingHR;

        double intensity = (double) (getHeartRate() - restingHR) / hrReserve;

        if (intensity < 0.5) setCurrentZone("Восстановление (<50%)");
        else if (intensity >= 0.5 && intensity < 0.6) setCurrentZone("Разминка (50-60%)");
        else if (intensity >= 0.6 && intensity < 0.7) setCurrentZone("Жиросжигание (60-70%)");
        else if (intensity >= 0.7 && intensity < 0.8) setCurrentZone("Аэробная (70-80%)");
        else if (intensity >= 0.8 && intensity < 0.9) setCurrentZone("Анаэробная (80-90%)");
        else setCurrentZone("МАКСИМАЛЬНАЯ НАГРУЗКА (>90%)");
    }
}