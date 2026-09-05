package co.id.service;

import co.id.model.Room;
import co.id.model.Subject;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.MajorReportItem;
import java.util.List;

public interface ReportService {
    public List<MajorReportItem> getMajorReport();
    public List<Room> getRoomReport();
    public List<ClassroomReportItem> getClassroomReport();
    public List<Subject> getSubjectReport(int majorId);
}
