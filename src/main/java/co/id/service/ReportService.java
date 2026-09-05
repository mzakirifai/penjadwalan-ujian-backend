package co.id.service;

import co.id.model.Room;
import co.id.model.report.MajorReportItem;
import java.util.List;

public interface ReportService {
    public List<MajorReportItem> getMajorReport();
    public List<Room> getRoomReport();
}
