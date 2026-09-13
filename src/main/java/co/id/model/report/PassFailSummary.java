package co.id.model.report;

/**
 * Ringkasan jumlah nilai yang lulus vs tidak lulus KKM, dipakai di
 * pie chart Dashboard.
 */
public class PassFailSummary {
    private int passCount;
    private int failCount;

    public PassFailSummary() {
    }

    public PassFailSummary(int passCount, int failCount) {
        this.passCount = passCount;
        this.failCount = failCount;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}