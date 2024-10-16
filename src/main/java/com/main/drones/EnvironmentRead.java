package com.main.drones;

public class EnvironmentRead {

    private float atmosphericPressure;
    private float solarRadiation;
    private float temperature;
    private float humidity;

    public float getAtmosphericPressure() {
        return atmosphericPressure;
    }

    public void setAtmosphericPressure(float atmosphericPressure) {
        this.atmosphericPressure = atmosphericPressure;
    }

    public float getSolarRadiation() {
        return solarRadiation;
    }

    public void setSolarRadiation(float solarRadiation) {
        this.solarRadiation = solarRadiation;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }
}
