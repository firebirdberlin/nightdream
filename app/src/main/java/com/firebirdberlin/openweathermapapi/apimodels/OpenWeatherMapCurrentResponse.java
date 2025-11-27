package com.firebirdberlin.openweathermapapi.apimodels;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenWeatherMapCurrentResponse {
    private ApiCoord coord;
    private List<Weather> weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Clouds clouds;
    private Rain rain; // Modified Rain model
    private Snow snow; // Modified Snow model
    private long dt;
    @SerializedName("sys")
    private CurrentSys currentSys; // Renamed to avoid conflict with forecast's Sys
    private long timezone;
    private int id; // City ID
    private String name; // City name
    private int cod; // Internal parameter, could be int or String

    public ApiCoord getCoord() {
        return coord;
    }

    public void setCoord(ApiCoord coord) {
        this.coord = coord;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Clouds getClouds() {
        return clouds;
    }

    public void setClouds(Clouds clouds) {
        this.clouds = clouds;
    }

    public Rain getRain() {
        return rain;
    }

    public void setRain(Rain rain) {
        this.rain = rain;
    }

    public Snow getSnow() {
        return snow;
    }

    public void setSnow(Snow snow) {
        this.snow = snow;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public CurrentSys getCurrentSys() {
        return currentSys;
    }

    public void setCurrentSys(CurrentSys currentSys) {
        this.currentSys = currentSys;
    }

    public long getTimezone() {
        return timezone;
    }

    public void setTimezone(long timezone) {
        this.timezone = timezone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCod() {
        return cod;
    }

    public void setCod(int cod) {
        this.cod = cod;
    }
}