package co.id.model.report;

/**
 * Bean khusus untuk kebutuhan ReportMajor.jrxml.
 * Field di sini HARUS sama persis namanya dengan Field yang didefinisikan
 * manual di Jasper Studio (Outline > Fields > Add Field), karena report
 * tidak connect ke database secara langsung.
 */
public class MajorReportItem {
    private String code;
    private String name;
    private String abbreviation;
    private int classCount;
    private int studentCount;

    public MajorReportItem() {
    }

    public MajorReportItem(String code, String name, String abbreviation, int classCount, int studentCount) {
        this.code = code;
        this.name = name;
        this.abbreviation = abbreviation;
        this.classCount = classCount;
        this.studentCount = studentCount;
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

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }
}
