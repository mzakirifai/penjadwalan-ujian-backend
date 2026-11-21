package co.id.service;

import co.id.model.ScheduleConflict;
import java.util.List;

/** Jadwal bentrok: kelas yang sama sudah punya ujian lain pada jam yang bertumpuk. */
public class ScheduleConflictException extends IllegalStateException {
    private final List<ScheduleConflict> conflicts;

    public ScheduleConflictException(String message, List<ScheduleConflict> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public List<ScheduleConflict> getConflicts() { return conflicts; }
}