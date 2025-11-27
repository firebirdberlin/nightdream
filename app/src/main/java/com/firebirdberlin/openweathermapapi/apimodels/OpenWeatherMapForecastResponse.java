package com.firebirdberlin.openweathermapapi.apimodels;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenWeatherMapForecastResponse {
    private String cod;
    private double message;
    private int cnt;
    private List<ListEntry> list;
    private ApiCity city; // Renamed to ApiCity

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public double getMessage() {
        return message;
    }

    public void setMessage(double message) {
        this.message = message;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public List<ListEntry> getList() {
        return list;
    }

    public void setList(List<ListEntry> list) {
        this.list = list;
    }

    public ApiCity getCity() { // Renamed to ApiCity
        return city;
    }

    public void setCity(ApiCity city) { // Renamed to ApiCity
        this.city = city;
    }
}