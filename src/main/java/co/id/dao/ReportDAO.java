package co.id.dao;

import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.MajorReportItem;
import java.util.List;

public interface ReportDAO {
    public List<MajorReportItem> getMajorReport();
    public List<Room> getRoomReport();
    public List<ClassroomReportItem> getClassroomReport();
    public List<Subject> getSubjectReport(int majorId);
    public List<Teacher> getTeacherReport();
    public List<Student> getStudentListReport(int classroomId);
}