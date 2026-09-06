package co.id.model.report;

/**
 * Bean untuk ReportParticipantCard.jrxml.
 * 1 item = 1 sesi ujian (1 mapel) yang diikuti siswa dalam periode ujian tertentu.
 * Field tanggal/jam sengaja String (sudah diformat di Java) untuk menghindari
 * bug Jaspersoft Studio pada java.time.LocalDate/LocalTime.
 */
public class ParticipantCardReportItem {
    private String date;
    private String startTime;
    private String endTime;
    private String subjectName;
    private String roomName;
    private String participantNumber;
    private String seatNumber;

    public ParticipantCardReportItem() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}