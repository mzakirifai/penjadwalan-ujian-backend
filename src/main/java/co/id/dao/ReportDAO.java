package co.id.dao;

import co.id.model.report.MajorReportItem;
import java.util.List;

public interface ReportDAO {
    public List<MajorReportItem> getMajorReport();
}