package co.id.model;

import java.time.LocalDateTime;

/** Jenis ujian (tabel mst_jenis_ujian), mis. kode "AAS" = "Asesmen Akhir Semester". */
public class ExamType {
    private int id;
    private String code;
    private String fullName;
    private boolean active = true;
    private int order;
    /** Jumlah jadwal yang memakai jenis ini (hanya diisi saat ditampilkan di menu Jenis Ujian). */
    private int usageCount;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return code;
    }
}