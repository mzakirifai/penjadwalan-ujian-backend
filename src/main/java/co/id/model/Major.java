package co.id.model;

import java.time.LocalDateTime;

public class Major {
    private int id;
    private String code;
    private String name;
    private String abbreviation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public Major() {
    }

    public Major(int id, String code, String name, String abbreviation, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.abbreviation = abbreviation;
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

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
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
        return "Jurusan[id ="+id+", kode_jurusan ="+code+", nama_jurusan ="+name+", singkatan ="+abbreviation+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", dibuat_olej="+updatedBy+"]";
    }
}
