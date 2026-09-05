package co.id.service.impl;

import co.id.dao.ReportDAO;
import co.id.dao.impl.ReportDAOImpl;
import co.id.model.report.MajorReportItem;
import co.id.service.ReportService;
import java.util.List;

public class ReportServiceImpl implements ReportService {
    private final ReportDAO reportDAO;

    public ReportServiceImpl() {
        reportDAO = new ReportDAOImpl();
    }

    @Override
    public List<MajorReportItem> getMajorReport() {
        return reportDAO.getMajorReport();
    }
}

