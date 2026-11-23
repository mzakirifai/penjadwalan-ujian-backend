package co.id.model.report;

/**
 * Bean untuk ReportAttendanceList.jrxml: satu baris = satu kursi di satu ruangan pada satu sesi.
 * Data ruangan dan pengawas diulang di tiap baris karena laporan dikelompokkan per ruangan
 * (satu ruangan = halaman baru). Semua field String agar aman dipakai di Jaspersoft Studio.
 */
public class RoomAttendanceReportItem {
    private String roomKey;
    private String roomName;
    private String classSummary;
    private String supervisorName;
    private String supervisorNip;
    private String totalStudents;
    private String seatLabel;
    private String participantNumber;
    private String nis;
    private String studentName;
    private String className;
    // Judul yang berbeda untuk daftar hadir ujian online (per kelas, tanpa ruangan & kursi)
    private String seatHeader = "No Kursi";
    private String supervisorTitle = "Pengawas";
    private String signerTitle = "Pengawas Ruang";

    public RoomAttendanceReportItem() {
    }

    /** Kunci pengelompokan; berisi id ruangan agar dua ruangan bernama sama tetap terpisah. */
    public String getRoomKey() { return roomKey; }
    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    /** Mis. "X TKJ 1 (18), XI TP (16)". */
    public String getClassSummary() { return classSummary; }
    public void setClassSummary(String classSummary) { this.classSummary = classSummary; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public String getSupervisorNip() { return supervisorNip; }
    public void setSupervisorNip(String supervisorNip) { this.supervisorNip = supervisorNip; }

    public String getTotalStudents() { return totalStudents; }
    public void setTotalStudents(String totalStudents) { this.totalStudents = totalStudents; }

    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }

    public String getParticipantNumber() { return participantNumber; }
    public void setParticipantNumber(String participantNumber) { this.participantNumber = participantNumber; }

    public String getNis() { return nis; }
    public void setNis(String nis) { this.nis = nis; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSeatHeader() { return seatHeader; }
    public void setSeatHeader(String seatHeader) { this.seatHeader = seatHeader; }

    public String getSupervisorTitle() { return supervisorTitle; }
    public void setSupervisorTitle(String supervisorTitle) { this.supervisorTitle = supervisorTitle; }

    public String getSignerTitle() { return signerTitle; }
    public void setSignerTitle(String signerTitle) { this.signerTitle = signerTitle; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}