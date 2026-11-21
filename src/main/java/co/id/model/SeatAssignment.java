package co.id.model;

/** Satu kursi di satu ruangan: siswa mana, dari jadwal (kelas + mapel) mana. */
public class SeatAssignment {
    private final int seatIndex;         // urutan kursi di ruangan, mulai 1
    private final Student student;
    private final ExamSchedule schedule;
    private Integer participantId;       // id baris trx_peserta jika siswa sudah terdaftar
    private String participantNumber;    // nomor peserta (ada jika sudah terdaftar atau sudah diberi nomor)

    public SeatAssignment(int seatIndex, Student student, ExamSchedule schedule) {
        this.seatIndex = seatIndex;
        this.student = student;
        this.schedule = schedule;
    }

    public int getSeatIndex() { return seatIndex; }
    public String getSeatLabel() { return String.format("K%02d", seatIndex); }
    public Student getStudent() { return student; }
    public ExamSchedule getSchedule() { return schedule; }
    public Integer getParticipantId() { return participantId; }
    public void setParticipantId(Integer participantId) { this.participantId = participantId; }
    public String getParticipantNumber() { return participantNumber; }
    public void setParticipantNumber(String participantNumber) { this.participantNumber = participantNumber; }

    public String getClassName() {
        return schedule != null && schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-";
    }
}