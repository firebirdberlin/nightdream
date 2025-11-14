package com.firebirdberlin.openweathermapapi.apimodels;

import java.util.List;

public class FindCityEntry {
    private int id;
    private String name;
    private ApiCoord coord; // Reuse existing ApiCoord
    private FindSys sys; // Specific Sys for this endpoint

    // Other fields (main, weather, wind, etc.) are ignored as they are not needed for your City model.

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

    public ApiCoord getCoord() {
        return coord;
    }

    public void setCoord(ApiCoord coord) {
        this.coord = coord;
    }

    public FindSys getSys() {
        return sys;
    }

    public void setSys(FindSys sys) {
        this.sys = sys;
    }
}