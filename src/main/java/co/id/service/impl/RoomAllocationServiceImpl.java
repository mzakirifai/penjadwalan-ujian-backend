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
        Map<String, List<ExamSchedule>> grouped = new LinkedHashMap<>();
        examScheduleDAO.getAllExamSchedulesDetailed().stream()
                .filter(s -> date.equals(s.getDate()) && s.getStartTime() != null && s.getEndTime() != null)
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
    public List<Room> getUsableRooms(ExamSession session) {
        Set<Integer> busy = roomAllocationDAO.getBusyRoomIds(session.getDate(), session.getStartTime(), session.getEndTime());
        return masterService.getAllRooms().stream()
                .filter(r -> r.getCapacity() > 0 && !busy.contains(r.getId()))
                .sorted(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
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
            Map<Integer, Student> students = new LinkedHashMap<>();
            if (schedule.getClassroom() != null) {
                for (Student student : studentDAO.getByClassroom(schedule.getClassroom().getId())) {
                    students.put(student.getId(), student);
                }
            }
            for (SeatRow row : rowsBySchedule.getOrDefault(schedule.getId(), List.of())) {
                if (!students.containsKey(row.studentId)) {
                    Student extra = new Student();
                    extra.setId(row.studentId);
                    extra.setNis(row.nis);
                    extra.setName(row.studentName);
                    students.put(row.studentId, extra);
                }
            }

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
        Map<String, SeatRow> existingByKey = new HashMap<>();
        for (SeatRow row : existingRows) {
            existingByKey.put(row.scheduleId + ":" + row.studentId, row);
        }
        Map<Integer, Integer> nextNumber = new HashMap<>();
        for (ExamSchedule schedule : schedules) {
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
        return plan;
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