package com.rental.model;

public class Zone {

    private int id;
    private String zoneName;
    private int slotCount;
    private String zoneStatus;

    public Zone(int id, String zoneName, int slotCount, String zoneStatus) {
        this.id = id;
        this.zoneName = zoneName;
        this.slotCount = slotCount;
        this.zoneStatus = zoneStatus;
    }

    public int getId() {
        return id;
    }

    public String getZoneName() {
        return zoneName;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public String getZoneStatus() {
        return zoneStatus;
    }

    public String getStatus() {
        return zoneStatus;
    }

    public void setZoneStatus(String zoneStatus) {
        this.zoneStatus = zoneStatus;
    }
}
