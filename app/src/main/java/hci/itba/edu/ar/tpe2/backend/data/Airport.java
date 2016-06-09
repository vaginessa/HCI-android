package hci.itba.edu.ar.tpe2.backend.data;

import java.io.Serializable;

public class Airport implements Serializable {
    private String id, description, time_zone;
    private City city;

    public Airport(String id, String description, String timezoneStr, City city) {
        this.id = id;
        this.description = description;
        this.time_zone = timezoneStr;
        this.city = city;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getTimezoneStr() {
        return time_zone;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }
}
