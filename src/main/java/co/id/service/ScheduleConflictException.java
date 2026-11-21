package co.id.service;

import co.id.model.RoomSuggestion;
import co.id.model.ScheduleConflict;
import java.util.List;

/** Jadwal bentrok. Membawa daftar bentrokan dan (jika ruangan yang bentrok) saran ruangan kosong. */
public class ScheduleConflictException extends IllegalStateException {
    private final List<ScheduleConflict> conflicts;
    private final List<RoomSuggestion> suggestions;
    private final boolean roomOnly;

    public ScheduleConflictException(String message, List<ScheduleConflict> conflicts,
                                     List<RoomSuggestion> suggestions, boolean roomOnly) {
        super(message);
        this.conflicts = conflicts;
        this.suggestions = suggestions;
        this.roomOnly = roomOnly;
    }

    public List<ScheduleConflict> getConflicts() { return conflicts; }
    public List<RoomSuggestion> getSuggestions() { return suggestions; }

    /** True jika satu-satunya yang bentrok adalah ruangan, jadi mengganti ruangan sudah cukup. */
    public boolean isRoomOnly() { return roomOnly; }
}