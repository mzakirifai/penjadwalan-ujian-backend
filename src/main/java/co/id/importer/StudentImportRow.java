package co.id.importer;

/**
 * Satu baris siswa hasil bacaan dari Excel (belum disimpan ke database).
 */
public class StudentImportRow {
    private final String sheetName;
    private final int rowNumber;       // nomor baris di Excel (mulai dari 1)
    private final String nis;
    private final String nisn;
    private final String name;
    private final String gender;       // "Laki-laki" / "Perempuan" / null
    private final String grade;        // tingkat: X / XI / XII (dari nama sheet), bisa null
    private final String classLabel;   // isi kolom Kelas di Excel, mis. "TKJ" atau "PH 1"

    public StudentImportRow(String sheetName, int rowNumber, String nis, String nisn, String name, String gender,
                            String grade, String classLabel) {
        this.sheetName = sheetName;
        this.rowNumber = rowNumber;
        this.nis = nis;
        this.nisn = nisn;
        this.name = name;
        this.gender = gender;
        this.grade = grade;
        this.classLabel = classLabel;
    }

    public String getSheetName() { return sheetName; }
    public int getRowNumber() { return rowNumber; }
    public String getNis() { return nis; }
    public String getNisn() { return nisn; }
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getGrade() { return grade; }
    public String getClassLabel() { return classLabel; }

    /**
     * Nama kelas lengkap menurut Excel, mis. sheet "X" + kelas "TKJ" -> "X TKJ".
     * Jika isi kolom Kelas sudah diawali tingkat (mis. "X TKJ 1"), dipakai apa adanya.
     */
    public String getFullClassName() {
        String label = classLabel == null ? "" : classLabel.trim();
        if (label.isEmpty()) {
            return "";
        }
        if (grade == null || grade.isEmpty()) {
            return label;
        }
        String upper = label.toUpperCase();
        if (upper.equals(grade) || upper.startsWith(grade + " ")) {
            return label;
        }
        return grade + " " + label;
    }
}