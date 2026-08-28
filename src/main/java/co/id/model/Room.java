package co.id.model;

import java.time.LocalDateTime;

public class Room {
    private int id;
    private String code;
    private String name;
    private int floor;
    private int capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public Room() {
    }

    public Room(int id, String code, String name, int floor, int capacity, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.floor = floor;
        this.capacity = capacity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    @Override
    public String toString(){
        return "Ruangan[id ="+id+", kode_ruangan ="+code+", nama_ruangan ="+name+", lantai ="+floor+", kapasitas ="+capacity+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", diubah_oleh="+updatedBy+"]";
    }
}
