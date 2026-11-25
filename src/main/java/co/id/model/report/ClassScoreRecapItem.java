package co.id.model.report;

/**
 * Bean untuk ReportClassScore.jrxml.
 * 1 item = 1 nilai siswa untuk 1 mapel dalam periode ujian tertentu.
 * Data sengaja dibiarkan flat (bukan pivot) — proses "memutar" jadi
 * tabel Siswa x Mapel dilakukan oleh komponen Crosstab di Jasper Studio,
 * bukan di sisi Java.
 */
public class ClassScoreRecapItem {
    /** Nomor urut siswa di laporan (1, 2, 3, ...), sama untuk semua nilai milik siswa yang sama. */
    private int rowNo;
    private String nisn;
    private String nis;
    private String studentName;
    private String subjectName;
    private int score;

    public ClassScoreRecapItem() {
    }

    public int getRowNo() {
        return rowNo;
    }

    public void setRowNo(int rowNo) {
        this.rowNo = rowNo;
    }

    public String getNisn() {
        return nisn;
    }

    public void setNisn(String nisn) {
        this.nisn = nisn;
    }

    public String getNis() {
        return nis;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}