package co.id.model.report;

import co.id.model.Major;

public class ClassroomReportItem {
    private String code;
    private String name;
    private String grade;
    private int studentCount;
    
    // Relasi Many-to-one
    private Major major;

    public ClassroomReportItem() {
    }

    public ClassroomReportItem(String code, String name, String grade, int studentCount, Major major) {
        this.code = code;
        this.name = name;
        this.grade = grade;
        this.studentCount = studentCount;
        this.major = major;
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

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
    }
}
