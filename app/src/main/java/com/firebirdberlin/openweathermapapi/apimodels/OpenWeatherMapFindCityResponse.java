package com.firebirdberlin.openweathermapapi.apimodels;

import java.util.List;

public class OpenWeatherMapFindCityResponse {
    private String message;
    private String cod;
    private int count;
    private List<FindCityEntry> list;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<FindCityEntry> getList() {
        return list;
    }

    public void setList(List<FindCityEntry> list) {
        this.list = list;
    }
}