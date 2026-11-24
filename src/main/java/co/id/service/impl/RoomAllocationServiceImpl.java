package co.id.service.impl;

import co.id.dao.ExamScheduleDAO;
import co.id.dao.RoomAllocationDAO;
import co.id.dao.RoomAllocationDAO.SeatRow;
import co.id.dao.RoomAllocationDAO.SeatWrite;
import co.id.dao.StudentDAO;
import co.id.dao.impl.ExamScheduleDAOImpl;
import co.id.dao.impl.RoomAllocationDAOImpl;
import co.id.dao.impl.StudentDAOImpl;
import co.id.model.ExamSchedule;
import co.id.model.ExamSession;
import co.id.model.Room;
import co.id.model.RoomAssignment;
import co.id.model.RoomAvailability;
import co.id.model.RoomPlan;
import co.id.model.SeatAssignment;
import co.id.model.Student;
import co.id.model.Teacher;
import co.id.model.TeachingAssignment;
import co.id.service.MasterService;
import co.id.service.RoomAllocationService;
import co.id.service.RoomMixer;
import co.id.service.TeachingAssignmentService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RoomAllocationServiceImpl implements RoomAllocationService {

    private final ExamScheduleDAO examScheduleDAO = new ExamScheduleDAOImpl();
    private final RoomAllocationDAO roomAllocationDAO = new RoomAllocationDAOImpl();
    private final StudentDAO studentDAO = new StudentDAOImpl();
    private final MasterService masterService = new MasterServiceImpl();
    private final TeachingAssignmentService teachingAssignmentService = new TeachingAssignmentServiceImpl();

    @Override
    public List<ExamSession> getSessions(LocalDate date) {
        return groupSessions(date, false); // ujian online tidak memakai ruangan
    }

    @Override
    public List<ExamSession> getOnlineSessions(LocalDate date) {
        return groupSessions(date, true);
    }

    private List<ExamSession> groupSessions(LocalDate date, boolean online) {
        Map<String, List<ExamSchedule>> grouped = new LinkedHashMap<>();
        examScheduleDAO.getAllExamSchedulesDetailed().stream()
                .filter(s -> date.equals(s.getDate()) && s.getStartTime() != null && s.getEndTime() != null)
                .filter(s -> s.isOnline() == online)
                .sorted(Comparator.comparing(ExamSchedule::getStartTime).thenComparing(ExamSchedule::getEndTime)
                        .thenComparingInt(ExamSchedule::getId))
                .forEach(s -> grouped.computeIfAbsent(s.getStartTime() + "-" + s.getEndTime(), k -> new ArrayList<>()).add(s));

        List<ExamSession> sessions = new ArrayList<>();
        for (List<ExamSchedule> schedules : grouped.values()) {
            ExamSchedule first = schedules.get(0);
            sessions.add(new ExamSession(date, first.getStartTime(), first.getEndTime(), schedules));
        }
        return sessions;
    }

    @Override
    public List<RoomAvailability> getRoomAvailability(ExamSession session) {
        Map<Integer, String> usages = roomAllocationDAO.getRoomUsages(session.getDate(), session.getStartTime(), session.getEndTime());
        return masterService.getAllRooms().stream()
                .filter(r -> r.getCapacity() > 0)
                .sorted(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .map(r -> new RoomAvailability(r, usages.get(r.getId())))
                .collect(Collectors.toList());
    }

    // Siswa satu jadwal: seluruh siswa kelasnya + peserta yang sudah terdaftar dari luar kelas
    private Map<Integer, Student> studentsOf(ExamSchedule schedule, List<SeatRow> registeredRows) {
        Map<Integer, Student> students = new LinkedHashMap<>();
        if (schedule.getClassroom() != null) {
            for (Student student : studentDAO.getByClassroom(schedule.getClassroom().getId())) {
                students.put(student.getId(), student);
            }
        }
        for (SeatRow row : registeredRows) {
            if (!students.containsKey(row.studentId)) {
                Student extra = new Student();
                extra.setId(row.studentId);
                extra.setNis(row.nis);
                extra.setName(row.studentName);
                students.put(row.studentId, extra);
            }
        }
        return students;
    }

    /** Hitung kursi yang dibutuhkan sesi ini (persis seperti generate) dan jumlah jadwal yang punya siswa. */
    private int[] seatDemand(ExamSession session) {
        List<Integer> scheduleIds = session.getSchedules().stream().map(ExamSchedule::getId).collect(Collectors.toList());
        Map<Integer, List<SeatRow>> rowsBySchedule = roomAllocationDAO.getSeatRows(scheduleIds).stream()
                .collect(Collectors.groupingBy(r -> r.scheduleId));
        int seats = 0;
        int groups = 0;
        for (ExamSchedule schedule : session.getSchedules()) {
            int size = studentsOf(schedule, rowsBySchedule.getOrDefault(schedule.getId(), List.of())).size();
            if (size > 0) {
                seats += size;
                groups++;
            }
        }
        return new int[]{seats, groups};
    }

    @Override
    public int countSeatsNeeded(ExamSession session) {
        return seatDemand(session)[0];
    }

    /*
     * Pilih ruangan kosong secara otomatis:
     *  1. jumlah ruangan sesedikit mungkin, tetapi minimal ceil(jumlah kelas / 3) karena satu ruangan
     *     maksimal berisi 3 kelas (aturan pencampuran);
     *  2. dari kombinasi dengan jumlah ruangan itu, kursi kosong paling sedikit;
     *  3. lalu ruangan yang tersebar di lantai paling sedikit, lalu urutan nama.
     * Bila kapasitas semua ruangan kosong tidak cukup, semua ruangan kosong dipilih (generate akan memberi peringatan).
     */
    @Override
    public List<Room> suggestRooms(ExamSession session) {
        List<Room> free = getRoomAvailability(session).stream()
                .filter(RoomAvailability::isAvailable)
                .map(RoomAvailability::getRoom)
                .sorted(Comparator.comparingInt(Room::getCapacity).reversed()
                        .thenComparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        int[] demand = seatDemand(session);
        int needed = demand[0];
        if (free.isEmpty() || needed == 0) {
            return List.of();
        }
        int totalFree = free.stream().mapToInt(Room::getCapacity).sum();
        if (totalFree <= needed) {
            return free;
        }

        int minRooms = Math.max(1, (demand[1] + 2) / 3);
        for (int k = minRooms; k <= free.size(); k++) {
            int best = bestCombination(free, k, needed);
            if (best >= 0) {
                return bestRooms;
            }
        }
        return free;
    }

    // hasil pencarian kombinasi terbaik (dipakai suggestRooms)
    private List<Room> bestRooms;
    private int bestCapacity;
    private int bestFloors;

    // Cari kombinasi k ruangan dengan kapasitas >= needed dan kapasitas total terkecil; -1 bila tidak ada
    private int bestCombination(List<Room> rooms, int k, int needed) {
        // batas atas: k ruangan terbesar pun tidak cukup -> mustahil
        int top = 0;
        for (int i = 0; i < k && i < rooms.size(); i++) {
            top += rooms.get(i).getCapacity();
        }
        if (top < needed) {
            return -1;
        }
        bestRooms = null;
        bestCapacity = Integer.MAX_VALUE;
        bestFloors = Integer.MAX_VALUE;
        search(rooms, k, needed, 0, new ArrayList<>(), 0);
        return bestRooms == null ? -1 : bestCapacity;
    }

    private void search(List<Room> rooms, int k, int needed, int from, List<Room> chosen, int capacity) {
        if (chosen.size() == k) {
            if (capacity < needed) {
                return;
            }
            int floors = (int) chosen.stream().map(Room::getFloor).distinct().count();
            if (capacity < bestCapacity || (capacity == bestCapacity && floors < bestFloors)) {
                bestCapacity = capacity;
                bestFloors = floors;
                bestRooms = new ArrayList<>(chosen);
            }
            return;
        }
        int remaining = k - chosen.size();
        for (int i = from; i <= rooms.size() - remaining; i++) {
            // ruangan diurutkan dari kapasitas terbesar: bila sisa ruangan terbesar pun tidak cukup, hentikan cabang ini
            int optimistic = capacity;
            for (int j = i; j < i + remaining; j++) {
                optimistic += rooms.get(j).getCapacity();
            }
            if (optimistic < needed) {
                break;
            }
            chosen.add(rooms.get(i));
            search(rooms, k, needed, i + 1, chosen, capacity + rooms.get(i).getCapacity());
            chosen.remove(chosen.size() - 1);
        }
    }

    @Override
    public RoomPlan generate(ExamSession session, List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Pilih minimal satu ruangan.");
        }

        List<ExamSchedule> schedules = session.getSchedules();
        List<Integer> scheduleIds = schedules.stream().map(ExamSchedule::getId).collect(Collectors.toList());
        List<SeatRow> existingRows = roomAllocationDAO.getSeatRows(scheduleIds);
        Map<Integer, List<SeatRow>> rowsBySchedule = existingRows.stream()
                .collect(Collectors.groupingBy(r -> r.scheduleId));

        RoomPlan plan = new RoomPlan(session.getDate(), session.getStartTime(), session.getEndTime(), schedules);

        // 1. kelompok = satu jadwal (kelas + mapel): seluruh siswa kelas + peserta yang sudah terdaftar
        List<RoomMixer.Group> groups = new ArrayList<>();
        Map<Integer, Integer> studentGroupCount = new HashMap<>();
        Map<Integer, String> studentNames = new HashMap<>();
        for (ExamSchedule schedule : schedules) {
            Map<Integer, Student> students = studentsOf(schedule, rowsBySchedule.getOrDefault(schedule.getId(), List.of()));

            List<Student> ordered = new ArrayList<>(students.values());
            ordered.sort(Comparator.comparing(s -> s.getName() == null ? "" : s.getName(), String.CASE_INSENSITIVE_ORDER));

            if (ordered.isEmpty()) {
                plan.getWarnings().add("Jadwal " + schedule.getSubjectLabel() + " ("
                        + (schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-")
                        + ") tidak memiliki siswa, dilewati.");
                continue;
            }
            for (Student student : ordered) {
                studentGroupCount.merge(student.getId(), 1, Integer::sum);
                studentNames.put(student.getId(), student.getName());
            }
            groups.add(new RoomMixer.Group(schedule, ordered));
        }
        if (groups.isEmpty()) {
            throw new IllegalStateException("Tidak ada siswa pada sesi ini. Pastikan kelas pada jadwal sudah memiliki siswa.");
        }
        studentGroupCount.forEach((id, count) -> {
            if (count > 1) {
                plan.getWarnings().add("Siswa " + studentNames.get(id) + " ada di lebih dari satu jadwal pada sesi ini, sehingga duduk di dua kursi.");
            }
        });

        // 2. campur: ruangan terbesar dipakai lebih dulu agar ruangan yang terpakai sesedikit mungkin
        List<Room> ordered = new ArrayList<>(rooms);
        ordered.sort(Comparator.comparingInt(Room::getCapacity).reversed().thenComparing(Room::getName));
        plan.getRooms().addAll(RoomMixer.mix(groups, ordered, plan.getWarnings()));

        // 3. nomor peserta: pakai yang sudah ada; siswa baru diberi nomor berikutnya (3 digit) per jadwal
        assignParticipantNumbers(plan, existingRows, rowsBySchedule);

        // 4. catatan untuk admin
        if (plan.getRooms().size() < rooms.size()) {
            plan.getWarnings().add("Hanya " + plan.getRooms().size() + " dari " + rooms.size()
                    + " ruangan terpilih yang dipakai; sisanya tidak diperlukan.");
        }
        int classCount = (int) groups.stream().map(g -> g.getSchedule().getClassroom() == null ? 0 : g.getSchedule().getClassroom().getId()).distinct().count();
        for (RoomAssignment assignment : plan.getRooms()) {
            if (classCount > 1 && assignment.getClassCounts().size() == 1) {
                plan.getWarnings().add("Ruangan " + assignment.getRoom().getName() + " hanya berisi satu kelas ("
                        + assignment.describeClasses() + ").");
            }
        }
        keepSavedSupervisors(plan);
        return plan;
    }

    // Nomor peserta: pakai yang sudah ada; siswa baru diberi nomor berikutnya (3 digit) per jadwal
    private void assignParticipantNumbers(RoomPlan plan, List<SeatRow> existingRows, Map<Integer, List<SeatRow>> rowsBySchedule) {
        Map<String, SeatRow> existingByKey = new HashMap<>();
        for (SeatRow row : existingRows) {
            existingByKey.put(row.scheduleId + ":" + row.studentId, row);
        }
        Map<Integer, Integer> nextNumber = new HashMap<>();
        for (ExamSchedule schedule : plan.getSchedules()) {
            int max = 0;
            for (SeatRow row : rowsBySchedule.getOrDefault(schedule.getId(), List.of())) {
                try {
                    max = Math.max(max, Integer.parseInt(row.participantNumber.trim()));
                } catch (Exception ignored) {
                    // nomor non-angka tidak ikut dihitung
                }
            }
            nextNumber.put(schedule.getId(), max + 1);
        }
        for (RoomAssignment assignment : plan.getRooms()) {
            for (SeatAssignment seat : assignment.getSeats()) {
                SeatRow row = existingByKey.get(seat.getSchedule().getId() + ":" + seat.getStudent().getId());
                if (row != null) {
                    seat.setParticipantId(row.participantId);
                    seat.setParticipantNumber(row.participantNumber);
                } else {
                    int number = nextNumber.merge(seat.getSchedule().getId(), 1, Integer::sum) - 1;
                    seat.setParticipantNumber(String.format("%03d", number));
                }
            }
        }
    }

    // ------------------------------------------------------------------ penempatan manual

    @Override
    public List<ExamSession> getSessionsInPeriod(String examType, String semester, String academicYear) {
        // tanggal + jam unik dari jadwal offline periode ini, lalu sesi lengkapnya (semua jadwal offline di jam itu)
        Map<String, ExamSchedule> slots = new LinkedHashMap<>();
        examScheduleDAO.getAllExamSchedulesDetailed().stream()
                .filter(s -> !s.isOnline() && s.getDate() != null && s.getStartTime() != null && s.getEndTime() != null)
                .filter(s -> java.util.Objects.equals(examType, s.getExamType()) && java.util.Objects.equals(semester, s.getSemester())
                        && java.util.Objects.equals(academicYear, s.getAcademicYear()))
                .sorted(Comparator.comparing(ExamSchedule::getDate).thenComparing(ExamSchedule::getStartTime)
                        .thenComparing(ExamSchedule::getEndTime))
                .forEach(s -> slots.putIfAbsent(s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime(), s));
        Map<LocalDate, List<ExamSession>> byDate = new HashMap<>();
        List<ExamSession> result = new ArrayList<>();
        for (ExamSchedule slot : slots.values()) {
            for (ExamSession session : byDate.computeIfAbsent(slot.getDate(), this::getSessions)) {
                if (session.getStartTime().equals(slot.getStartTime()) && session.getEndTime().equals(slot.getEndTime())) {
                    result.add(session);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Integer, Integer> getStudentCounts(ExamSession session) {
        List<Integer> scheduleIds = session.getSchedules().stream().map(ExamSchedule::getId).collect(Collectors.toList());
        Map<Integer, List<SeatRow>> rowsBySchedule = roomAllocationDAO.getSeatRows(scheduleIds).stream()
                .collect(Collectors.groupingBy(r -> r.scheduleId));
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (ExamSchedule schedule : session.getSchedules()) {
            counts.put(schedule.getId(), studentsOf(schedule, rowsBySchedule.getOrDefault(schedule.getId(), List.of())).size());
        }
        return counts;
    }

    @Override
    public RoomPlan buildManual(ExamSession session, Map<Integer, Map<Integer, Integer>> distribution, List<Room> rooms) {
        Map<Integer, Room> roomById = new LinkedHashMap<>();
        for (Room room : rooms) {
            roomById.put(room.getId(), room);
        }
        List<ExamSchedule> schedules = session.getSchedules();
        List<Integer> scheduleIds = schedules.stream().map(ExamSchedule::getId).collect(Collectors.toList());
        List<SeatRow> existingRows = roomAllocationDAO.getSeatRows(scheduleIds);
        Map<Integer, List<SeatRow>> rowsBySchedule = existingRows.stream().collect(Collectors.groupingBy(r -> r.scheduleId));

        RoomPlan plan = new RoomPlan(session.getDate(), session.getStartTime(), session.getEndTime(), schedules);
        Map<Integer, RoomAssignment> assignments = new LinkedHashMap<>();
        for (Room room : rooms) {
            assignments.put(room.getId(), new RoomAssignment(room));
        }

        for (ExamSchedule schedule : schedules) {
            List<Student> students = new ArrayList<>(studentsOf(schedule,
                    rowsBySchedule.getOrDefault(schedule.getId(), List.of())).values());
            students.sort(Comparator.comparing(s -> s.getName() == null ? "" : s.getName(), String.CASE_INSENSITIVE_ORDER));
            String label = (schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-") + " - "
                    + schedule.getSubjectLabel();
            int next = 0;
            for (Map.Entry<Integer, Integer> entry : distribution.getOrDefault(schedule.getId(), Map.of()).entrySet()) {
                int count = entry.getValue() == null ? 0 : entry.getValue();
                if (count < 0) {
                    throw new IllegalArgumentException("Jumlah siswa tidak boleh negatif (" + label + ").");
                }
                RoomAssignment target = assignments.get(entry.getKey());
                if (target == null && count > 0) {
                    throw new IllegalArgumentException("Ruangan untuk " + label + " tidak ada di daftar ruangan terpilih.");
                }
                if (next + count > students.size()) {
                    throw new IllegalArgumentException(label + ": dibagi " + (next + count) + " siswa, padahal hanya "
                            + students.size() + " siswa.");
                }
                for (int i = 0; i < count; i++) {
                    target.getSeats().add(new SeatAssignment(target.getSeats().size() + 1, students.get(next++), schedule));
                }
            }
            if (next < students.size()) {
                plan.getWarnings().add((students.size() - next) + " siswa " + label + " belum mendapat ruangan.");
            }
        }

        for (RoomAssignment assignment : assignments.values()) {
            if (assignment.getFilled() == 0) {
                continue;
            }
            RoomMixer.arrangeSeats(assignment);
            plan.getRooms().add(assignment);
            if (assignment.getFilled() > assignment.getRoom().getCapacity()) {
                plan.getWarnings().add("Ruangan " + assignment.getRoom().getName() + " berisi " + assignment.getFilled()
                        + " siswa, melebihi kapasitas " + assignment.getRoom().getCapacity() + ".");
            }
        }
        if (plan.getRooms().isEmpty()) {
            throw new IllegalStateException("Belum ada siswa yang ditempatkan ke ruangan.");
        }
        assignParticipantNumbers(plan, existingRows, rowsBySchedule);
        keepSavedSupervisors(plan);
        return plan;
    }

    /**
     * Penempatan dibuat ulang: pengawas yang sudah tersimpan untuk ruangan yang tetap dipakai tidak hilang
     * (mis. hasil impor jadwal pengawas atau pilihan manual sebelumnya).
     */
    private void keepSavedSupervisors(RoomPlan plan) {
        Map<Integer, Teacher> saved = roomAllocationDAO.getSupervisors(plan.getDate(), plan.getStartTime(), plan.getEndTime());
        for (RoomAssignment assignment : plan.getRooms()) {
            Teacher teacher = saved.get(assignment.getRoom().getId());
            if (teacher != null && assignment.getSupervisor() == null) {
                assignment.setSupervisor(teacher);
            }
        }
    }

    @Override
    public void moveSeat(RoomPlan plan, SeatAssignment seat, Room target) {
        RoomAssignment from = null;
        for (RoomAssignment assignment : plan.getRooms()) {
            if (assignment.getSeats().contains(seat)) {
                from = assignment;
            }
        }
        if (from == null) {
            throw new IllegalArgumentException("Siswa tidak ditemukan di penempatan ini.");
        }
        if (from.getRoom().getId() == target.getId()) {
            return;
        }
        RoomAssignment to = null;
        for (RoomAssignment assignment : plan.getRooms()) {
            if (assignment.getRoom().getId() == target.getId()) {
                to = assignment;
            }
        }
        if (to == null) {
            to = new RoomAssignment(target);
            plan.getRooms().add(to);
        }
        if (to.getFilled() >= target.getCapacity()) {
            throw new IllegalStateException("Ruangan " + target.getName() + " sudah penuh (" + target.getCapacity() + " kursi).");
        }
        from.getSeats().remove(seat);
        to.getSeats().add(seat);
        rearrange(from);
        rearrange(to);
        if (from.getFilled() == 0) {
            plan.getRooms().remove(from);
        }
    }

    // Susun ulang kursi satu ruangan (urut nama per jadwal, disebar), nomor peserta tetap
    private static void rearrange(RoomAssignment assignment) {
        Map<String, SeatAssignment> before = new HashMap<>();
        for (SeatAssignment seat : assignment.getSeats()) {
            before.put(seat.getSchedule().getId() + ":" + seat.getStudent().getId(), seat);
        }
        assignment.getSeats().sort(Comparator.comparing(
                (SeatAssignment s) -> s.getStudent().getName() == null ? "" : s.getStudent().getName(),
                String.CASE_INSENSITIVE_ORDER));
        RoomMixer.arrangeSeats(assignment);
        for (SeatAssignment seat : assignment.getSeats()) {
            SeatAssignment old = before.get(seat.getSchedule().getId() + ":" + seat.getStudent().getId());
            if (old != null) {
                seat.setParticipantId(old.getParticipantId());
                seat.setParticipantNumber(old.getParticipantNumber());
            }
        }
    }

    @Override
    public List<String> applyLayout(RoomPlan source, List<ExamSession> targets, String currentUsername, String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengatur penempatan ruangan.");
        }
        // pola: kelas -> (ruangan -> jumlah siswa), urutan ruangan sesuai rencana sumber
        Map<Integer, LinkedHashMap<Integer, Integer>> pattern = new LinkedHashMap<>();
        Map<Integer, Room> roomById = new LinkedHashMap<>();
        for (RoomAssignment assignment : source.getRooms()) {
            roomById.put(assignment.getRoom().getId(), assignment.getRoom());
            for (SeatAssignment seat : assignment.getSeats()) {
                if (seat.getSchedule().getClassroom() == null) {
                    continue;
                }
                pattern.computeIfAbsent(seat.getSchedule().getClassroom().getId(), k -> new LinkedHashMap<>())
                        .merge(assignment.getRoom().getId(), 1, Integer::sum);
            }
        }
        List<Room> rooms = new ArrayList<>(roomById.values());
        java.time.format.DateTimeFormatter dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<String> report = new ArrayList<>();
        for (ExamSession target : targets) {
            String name = target.getDate().format(dateFormat) + " " + target.getStartTime() + "-" + target.getEndTime();
            if (target.getDate().equals(source.getDate()) && target.getStartTime().equals(source.getStartTime())
                    && target.getEndTime().equals(source.getEndTime())) {
                continue;
            }
            try {
                Map<Integer, Integer> sizes = getStudentCounts(target);
                Map<Integer, Map<Integer, Integer>> distribution = new LinkedHashMap<>();
                List<String> notes = new ArrayList<>();
                for (ExamSchedule schedule : target.getSchedules()) {
                    int size = sizes.getOrDefault(schedule.getId(), 0);
                    LinkedHashMap<Integer, Integer> classPattern = schedule.getClassroom() == null ? null
                            : pattern.get(schedule.getClassroom().getId());
                    if (size == 0) {
                        continue;
                    }
                    if (classPattern == null) {
                        notes.add(schedule.getClassroom().getName() + " tidak ada di susunan sumber, belum ditempatkan");
                        continue;
                    }
                    distribution.put(schedule.getId(), fit(classPattern, size));
                }
                if (distribution.isEmpty()) {
                    report.add("DILEWATI " + name + ": tidak ada kelas yang cocok dengan susunan sumber.");
                    continue;
                }
                RoomPlan plan = buildManual(target, distribution, rooms);
                Map<Integer, Teacher> supervisors = roomAllocationDAO.getSupervisors(target.getDate(),
                        target.getStartTime(), target.getEndTime());
                for (RoomAssignment assignment : plan.getRooms()) {
                    assignment.setSupervisor(supervisors.get(assignment.getRoom().getId()));
                }
                save(plan, currentUsername, currentUserRole);
                report.add("OK " + name + ": " + plan.getTotalStudents() + " siswa di " + plan.getRooms().size()
                        + " ruangan" + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")"));
            } catch (Exception ex) {
                report.add("DILEWATI " + name + ": " + ex.getMessage());
            }
        }
        return report;
    }

    // Sesuaikan pola jumlah per ruangan dengan jumlah siswa kelas di sesi tujuan (selisih ditambah/dikurangi di ruangan terakhir)
    private static Map<Integer, Integer> fit(LinkedHashMap<Integer, Integer> classPattern, int size) {
        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>(classPattern);
        int total = result.values().stream().mapToInt(Integer::intValue).sum();
        List<Integer> keys = new ArrayList<>(result.keySet());
        int diff = size - total;
        for (int i = keys.size() - 1; i >= 0 && diff != 0; i--) {
            int current = result.get(keys.get(i));
            int changed = Math.max(0, current + diff);
            diff -= changed - current;
            result.put(keys.get(i), changed);
            if (diff > 0) {
                break;
            }
        }
        return result;
    }

    @Override
    public RoomPlan loadSaved(ExamSession session) {
        List<ExamSchedule> schedules = session.getSchedules();
        Map<Integer, ExamSchedule> scheduleById = schedules.stream().collect(Collectors.toMap(ExamSchedule::getId, s -> s));
        List<SeatRow> rows = roomAllocationDAO.getSeatRows(new ArrayList<>(scheduleById.keySet())).stream()
                .filter(r -> r.roomId != null).collect(Collectors.toList());
        if (rows.isEmpty()) {
            return null;
        }

        Map<Integer, Room> roomById = masterService.getAllRooms().stream().collect(Collectors.toMap(Room::getId, r -> r, (a, b) -> a));
        Map<Integer, Teacher> supervisors = roomAllocationDAO.getSupervisors(session.getDate(), session.getStartTime(), session.getEndTime());

        Map<Integer, List<SeatRow>> byRoom = rows.stream().collect(Collectors.groupingBy(r -> r.roomId));
        RoomPlan plan = new RoomPlan(session.getDate(), session.getStartTime(), session.getEndTime(), schedules);

        List<Integer> roomIds = new ArrayList<>(byRoom.keySet());
        roomIds.sort(Comparator.comparing(id -> roomById.containsKey(id) ? roomById.get(id).getName() : "", String.CASE_INSENSITIVE_ORDER));
        for (Integer roomId : roomIds) {
            Room room = roomById.get(roomId);
            if (room == null) {
                continue;
            }
            RoomAssignment assignment = new RoomAssignment(room);
            assignment.setSupervisor(supervisors.get(roomId));

            List<SeatRow> seatRows = new ArrayList<>(byRoom.get(roomId));
            seatRows.sort(Comparator.comparingInt(r -> seatIndexOf(r.seatNumber)));
            int fallback = 1;
            for (SeatRow row : seatRows) {
                Student student = new Student();
                student.setId(row.studentId);
                student.setNis(row.nis);
                student.setName(row.studentName);
                int index = seatIndexOf(row.seatNumber);
                SeatAssignment seat = new SeatAssignment(index > 0 ? index : fallback, student, scheduleById.get(row.scheduleId));
                seat.setParticipantId(row.participantId);
                seat.setParticipantNumber(row.participantNumber);
                assignment.getSeats().add(seat);
                fallback++;
            }
            plan.getRooms().add(assignment);
        }
        return plan;
    }

    private static int seatIndexOf(String seatNumber) {
        if (seatNumber == null) {
            return 0;
        }
        String digits = seatNumber.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    @Override
    public void assignSupervisorsAutomatically(RoomPlan plan) {
        Set<Integer> busy = roomAllocationDAO.getBusyTeacherIds(plan.getDate(), plan.getStartTime(), plan.getEndTime());
        Map<Integer, Integer> load = roomAllocationDAO.getSupervisionCounts(plan.getDate());
        List<TeachingAssignment> assignments = teachingAssignmentService.getAllTeachingAssignments();
        List<Teacher> teachers = masterService.getAllTeachers();

        Set<Integer> usedInPlan = new HashSet<>();
        for (RoomAssignment room : plan.getRooms()) {
            room.setSupervisor(null);

            Set<ExamSchedule> schedulesInRoom = room.getSeats().stream().map(SeatAssignment::getSchedule)
                    .filter(s -> s != null).collect(Collectors.toSet());

            Teacher best = null;
            for (Teacher teacher : teachers) {
                if (busy.contains(teacher.getId()) || usedInPlan.contains(teacher.getId())
                        || teachesIn(teacher, schedulesInRoom, assignments)) {
                    continue;
                }
                if (best == null || load.getOrDefault(teacher.getId(), 0) < load.getOrDefault(best.getId(), 0)
                        || (load.getOrDefault(teacher.getId(), 0).equals(load.getOrDefault(best.getId(), 0))
                            && teacher.getName().compareToIgnoreCase(best.getName()) < 0)) {
                    best = teacher;
                }
            }

            if (best == null) {
                plan.getWarnings().add("Tidak ada guru yang bisa mengawasi ruangan " + room.getRoom().getName()
                        + " (semua bertugas di sesi lain atau mengajar mapel di ruangan itu). Pilih manual.");
            } else {
                room.setSupervisor(best);
                usedInPlan.add(best.getId());
                load.merge(best.getId(), 1, Integer::sum);
            }
        }
    }

    // Guru tidak mengawasi ujian mapel yang ia ajar sendiri (kelas + mapel yang sama; jika ujian berkode, kodenya juga)
    private boolean teachesIn(Teacher teacher, Set<ExamSchedule> schedules, List<TeachingAssignment> assignments) {
        for (TeachingAssignment ta : assignments) {
            if (ta.getTeacher().getId() != teacher.getId()) {
                continue;
            }
            for (ExamSchedule schedule : schedules) {
                if (schedule.getSubject() != null && schedule.getClassroom() != null
                        && ta.getSubject().getId() == schedule.getSubject().getId()
                        && ta.getClassroom().getId() == schedule.getClassroom().getId()
                        && (schedule.getTeacherCode() == null || schedule.getTeacherCode().equals(ta.getTeacherCode()))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ impor jadwal pengawas

    private static final java.time.format.DateTimeFormatter DMY = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.time.format.DateTimeFormatter HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    /** Data satu sesi yang dibutuhkan untuk memeriksa impor pengawas. */
    private static final class SessionCheck {
        ExamSession session;
        Map<Integer, Teacher> placedRooms = new LinkedHashMap<>(); // ruangan di penempatan -> pengawas tersimpan
        Set<Integer> busyTeachers = new HashSet<>();
    }

    private SessionCheck sessionCheck(LocalDate date, LocalTime start, LocalTime end, Map<LocalDate, List<ExamSession>> cache) {
        SessionCheck check = new SessionCheck();
        for (ExamSession s : cache.computeIfAbsent(date, this::getSessions)) {
            if (s.getStartTime().equals(start) && s.getEndTime().equals(end)) {
                check.session = s;
            }
        }
        if (check.session != null) {
            RoomPlan plan = loadSaved(check.session);
            if (plan != null) {
                for (RoomAssignment room : plan.getRooms()) {
                    check.placedRooms.put(room.getRoom().getId(), room.getSupervisor());
                }
            }
            check.busyTeachers = roomAllocationDAO.getBusyTeacherIds(date, start, end);
        }
        return check;
    }

    private static String roomKey(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    @Override
    public List<co.id.importer.ImportItem<co.id.model.SupervisorAssignment>> previewSupervisorImport(
            List<co.id.importer.SupervisorScheduleParser.Entry> entries, Map<Integer, String> legend, Map<Integer, Teacher> mapping) {
        Map<String, Room> roomByKey = new HashMap<>();
        for (Room room : masterService.getAllRooms()) {
            roomByKey.putIfAbsent(roomKey(room.getName()), room);
        }

        // kelompokkan per sesi (tanggal + jam persis)
        Map<String, List<co.id.importer.SupervisorScheduleParser.Entry>> bySession = new LinkedHashMap<>();
        for (var e : entries) {
            bySession.computeIfAbsent(e.date() + "|" + e.start() + "|" + e.end(), k -> new ArrayList<>()).add(e);
        }

        Map<LocalDate, List<ExamSession>> cache = new HashMap<>();
        List<co.id.importer.ImportItem<co.id.model.SupervisorAssignment>> items = new ArrayList<>();
        for (List<co.id.importer.SupervisorScheduleParser.Entry> group : bySession.values()) {
            var first = group.get(0);
            SessionCheck check = sessionCheck(first.date(), first.start(), first.end(), cache);

            // pengawas akhir sesi ini = yang tersimpan untuk ruangan di luar file + yang ada di file
            Set<Integer> roomsInFile = new HashSet<>();
            for (var e : group) {
                Room room = roomByKey.get(roomKey(e.roomLabel()));
                if (room != null) {
                    roomsInFile.add(room.getId());
                }
            }
            Map<Integer, List<String>> roomsPerTeacher = new HashMap<>();
            check.placedRooms.forEach((roomId, teacher) -> {
                if (teacher != null && !roomsInFile.contains(roomId)) {
                    roomsPerTeacher.computeIfAbsent(teacher.getId(), k -> new ArrayList<>()).add("ruangan lain (tersimpan)");
                }
            });
            for (var e : group) {
                Teacher t = mapping.get(e.number());
                if (t != null) {
                    roomsPerTeacher.computeIfAbsent(t.getId(), k -> new ArrayList<>()).add(e.roomLabel());
                }
            }

            for (var e : group) {
                Room room = roomByKey.get(roomKey(e.roomLabel()));
                String fileName = legend.get(e.number());
                Teacher teacher = mapping.get(e.number());
                String error = null;
                String warning = null;

                if (room == null) {
                    error = "Ruangan " + e.roomLabel() + " tidak ada di menu Ruangan";
                } else if (check.session == null) {
                    error = "Tidak ada jadwal ujian offline pada " + e.date().format(DMY) + " jam "
                            + e.start().format(HM) + "-" + e.end().format(HM);
                } else if (!check.placedRooms.containsKey(room.getId())) {
                    error = room.getName() + " belum dipakai di Penempatan Ruangan sesi ini (buat/simpan penempatan dulu)";
                } else if (fileName == null) {
                    error = "No. " + e.number() + " tidak ada di daftar nama pengawas di file";
                } else if (teacher == null) {
                    error = "No. " + e.number() + " (" + fileName + ") belum dipasangkan ke data Guru";
                } else if (roomsPerTeacher.getOrDefault(teacher.getId(), List.of()).size() > 1) {
                    List<String> others = new ArrayList<>(roomsPerTeacher.get(teacher.getId()));
                    others.remove(e.roomLabel());
                    error = "Pengawas no. " + e.number() + " dobel di sesi ini, juga di " + String.join(", ", others);
                } else if (check.busyTeachers.contains(teacher.getId())) {
                    error = teacher.getName() + " sudah mengawas di sesi lain yang jamnya bertumpuk";
                } else {
                    Teacher previous = check.placedRooms.get(room.getId());
                    if (previous != null && previous.getId() != teacher.getId()) {
                        warning = "menggantikan " + previous.getName();
                    }
                }

                co.id.model.SupervisorAssignment payload = room == null || teacher == null ? null
                        : new co.id.model.SupervisorAssignment(e.date(), e.start(), e.end(), room, teacher);
                items.add(new co.id.importer.ImportItem<>(e.sheet(), e.rowNumber(), List.of(
                        e.date().format(DMY),
                        e.start().format(HM) + "-" + e.end().format(HM),
                        room != null ? room.getName() : e.roomLabel(),
                        String.valueOf(e.number()),
                        teacher != null ? teacher.getName() : (fileName != null ? fileName + " (?)" : "-")),
                        payload, error, warning));
            }
        }
        return items;
    }

    @Override
    public int importSupervisors(List<co.id.model.SupervisorAssignment> assignments, String currentUsername, String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengatur pengawas ruangan.");
        }
        if (assignments == null || assignments.isEmpty()) {
            return 0;
        }
        // cek ulang tepat sebelum menyimpan (data bisa berubah sejak pratinjau dibuka)
        Map<String, List<co.id.model.SupervisorAssignment>> bySession = new LinkedHashMap<>();
        for (var a : assignments) {
            bySession.computeIfAbsent(a.getDate() + "|" + a.getStartTime() + "|" + a.getEndTime(), k -> new ArrayList<>()).add(a);
        }
        Map<LocalDate, List<ExamSession>> cache = new HashMap<>();
        for (List<co.id.model.SupervisorAssignment> group : bySession.values()) {
            var first = group.get(0);
            String when = first.getDate().format(DMY) + " " + first.getStartTime().format(HM) + "-" + first.getEndTime().format(HM);
            SessionCheck check = sessionCheck(first.getDate(), first.getStartTime(), first.getEndTime(), cache);
            if (check.session == null) {
                throw new IllegalStateException("Sesi " + when + " tidak ada lagi di jadwal ujian offline.");
            }
            Map<Integer, Integer> finalSupervisor = new HashMap<>();
            check.placedRooms.forEach((roomId, t) -> finalSupervisor.put(roomId, t == null ? null : t.getId()));
            for (var a : group) {
                if (!check.placedRooms.containsKey(a.getRoom().getId())) {
                    throw new IllegalStateException(a.getRoom().getName() + " belum dipakai di penempatan sesi " + when + ".");
                }
                if (check.busyTeachers.contains(a.getTeacher().getId())) {
                    throw new IllegalStateException(a.getTeacher().getName() + " sudah mengawas di sesi lain yang bertumpuk dengan " + when + ".");
                }
                finalSupervisor.put(a.getRoom().getId(), a.getTeacher().getId());
            }
            Set<Integer> seen = new HashSet<>();
            for (Integer teacherId : finalSupervisor.values()) {
                if (teacherId != null && !seen.add(teacherId)) {
                    throw new IllegalStateException("Ada pengawas yang dobel di sesi " + when + ".");
                }
            }
        }
        return roomAllocationDAO.saveSupervisors(assignments, currentUsername);
    }

    @Override
    public void save(RoomPlan plan, String currentUsername, String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengatur penempatan ruangan.");
        }
        if (plan == null || plan.getRooms().isEmpty()) {
            throw new IllegalArgumentException("Belum ada penempatan untuk disimpan.");
        }

        Set<Integer> busyRooms = roomAllocationDAO.getBusyRoomIds(plan.getDate(), plan.getStartTime(), plan.getEndTime());
        Set<Integer> busyTeachers = roomAllocationDAO.getBusyTeacherIds(plan.getDate(), plan.getStartTime(), plan.getEndTime());

        List<SeatWrite> writes = new ArrayList<>();
        Map<Integer, Integer> supervisorByRoom = new LinkedHashMap<>();
        Set<Integer> usedTeachers = new HashSet<>();

        for (RoomAssignment assignment : plan.getRooms()) {
            Room room = assignment.getRoom();
            if (assignment.getFilled() > room.getCapacity()) {
                throw new IllegalStateException("Ruangan " + room.getName() + " berisi " + assignment.getFilled()
                        + " siswa, melebihi kapasitas " + room.getCapacity() + ".");
            }
            if (busyRooms.contains(room.getId())) {
                throw new IllegalStateException("Ruangan " + room.getName() + " sudah dipakai sesi lain yang waktunya bertumpuk.");
            }

            Teacher supervisor = assignment.getSupervisor();
            if (supervisor != null) {
                if (busyTeachers.contains(supervisor.getId())) {
                    throw new IllegalStateException("Pengawas " + supervisor.getName()
                            + " sudah bertugas di sesi lain yang waktunya bertumpuk.");
                }
                if (!usedTeachers.add(supervisor.getId())) {
                    throw new IllegalStateException("Pengawas " + supervisor.getName() + " dipilih untuk lebih dari satu ruangan.");
                }
            }
            supervisorByRoom.put(room.getId(), supervisor != null ? supervisor.getId() : null);

            for (SeatAssignment seat : assignment.getSeats()) {
                SeatWrite write = new SeatWrite();
                write.participantId = seat.getParticipantId();
                write.scheduleId = seat.getSchedule().getId();
                write.studentId = seat.getStudent().getId();
                write.participantNumber = seat.getParticipantNumber();
                write.roomId = room.getId();
                write.seatNumber = seat.getSeatLabel();
                writes.add(write);
            }
        }

        List<Integer> scheduleIds = plan.getSchedules().stream().map(ExamSchedule::getId).collect(Collectors.toList());
        roomAllocationDAO.saveAllocation(plan.getDate(), plan.getStartTime(), plan.getEndTime(), scheduleIds,
                writes, supervisorByRoom, currentUsername);
    }
}