package co.id.service;

import co.id.model.ExamSchedule;
import co.id.model.Room;
import co.id.model.RoomAssignment;
import co.id.model.SeatAssignment;
import co.id.model.Student;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritma ruangan campur.
 *
 * Aturan (dari sekolah): satu ruangan berisi campuran 2 sampai 3 kelompok (kelas), diutamakan beda tingkat
 * (X dan XI), siswa duduk berselingan, dan siswa satu kelas boleh terpecah ke beberapa ruangan.
 * Cara kerja, ruangan demi ruangan:
 *   1. pilih kelompok yang sisa siswanya terbanyak, lalu kelompok kedua (diutamakan beda tingkat, lalu beda mapel);
 *      kelompok ketiga hanya ditambah bila dua kelompok itu belum memenuhi kapasitas;
 *   2. bagi kapasitas serata mungkin antar kelompok (kelompok kecil diambil semuanya, sisanya dibagi rata);
 *   3. susun kursi berselingan antar kelompok.
 * Bila sesi berisi paling banyak 3 jadwal, langkah 1-2 diganti {@link #mixProportional}: tiap jadwal dibagi
 * sebanding kapasitas ke semua ruangan yang dipakai, sehingga tiap ruangan berisi semua jadwal (tetap maks. 3).
 * Setelah semua ruangan terisi, kursi tiap ruangan disusun ulang ({@link #arrangeSeats}) supaya siswa satu jadwal
 * (soal yang sama) tersebar rata dari kursi pertama sampai terakhir, tidak menumpuk di ujung ruangan.
 * Di dalam satu jadwal, siswa tetap berurutan nama.
 */
public class RoomMixer {

    public static class Group {
        private final ExamSchedule schedule;
        private final List<Student> students;

        public Group(ExamSchedule schedule, List<Student> students) {
            this.schedule = schedule;
            this.students = students;
        }

        public ExamSchedule getSchedule() { return schedule; }
        public List<Student> getStudents() { return students; }

        String grade() {
            return schedule.getClassroom() != null && schedule.getClassroom().getGrade() != null
                    ? schedule.getClassroom().getGrade() : "";
        }

        int subjectId() {
            return schedule.getSubject() != null ? schedule.getSubject().getId() : 0;
        }
    }

    public static List<RoomAssignment> mix(List<Group> groups, List<Room> rooms, List<String> warnings) {
        int total = groups.stream().mapToInt(g -> g.getStudents().size()).sum();
        int totalCapacity = rooms.stream().mapToInt(Room::getCapacity).sum();
        if (total > totalCapacity) {
            throw new IllegalStateException("Kapasitas ruangan yang dipilih (" + totalCapacity
                    + " kursi) kurang dari jumlah siswa (" + total + "). Pilih ruangan tambahan.");
        }

        if (groups.size() <= 3) {
            List<RoomAssignment> proportional = mixProportional(groups, rooms);
            for (RoomAssignment assignment : proportional) {
                arrangeSeats(assignment);
            }
            return proportional;
        }

        Map<Group, Deque<Student>> remaining = new LinkedHashMap<>();
        for (Group group : groups) {
            remaining.put(group, new ArrayDeque<>(group.getStudents()));
        }

        List<RoomAssignment> result = new ArrayList<>();
        for (Room room : rooms) {
            List<Group> active = activeGroups(remaining);
            if (active.isEmpty()) {
                break;
            }
            RoomAssignment assignment = new RoomAssignment(room);
            fillRoom(assignment, active, remaining);
            result.add(assignment);
        }

        // Cadangan: jika masih ada siswa (banyak kelas kecil, batas 3 kelas per ruangan membuat kursi tersisa),
        // isi kursi kosong yang ada tanpa batas jumlah kelas, dan beri peringatan.
        if (!activeGroups(remaining).isEmpty()) {
            for (RoomAssignment assignment : result) {
                int free = assignment.getRoom().getCapacity() - assignment.getFilled();
                int before = assignment.getClassCounts().size();
                for (Group group : activeGroups(remaining)) {
                    Deque<Student> queue = remaining.get(group);
                    while (free > 0 && !queue.isEmpty()) {
                        assignment.getSeats().add(new SeatAssignment(assignment.getFilled() + 1, queue.poll(), group.getSchedule()));
                        free--;
                    }
                }
                if (assignment.getClassCounts().size() > before && assignment.getClassCounts().size() > 3) {
                    warnings.add("Ruangan " + assignment.getRoom().getName() + " berisi lebih dari 3 kelas karena banyak kelas kecil.");
                }
            }
            if (!activeGroups(remaining).isEmpty()) {
                throw new IllegalStateException("Tidak semua siswa tertampung. Pilih ruangan tambahan.");
            }
        }

        for (RoomAssignment assignment : result) {
            arrangeSeats(assignment);
        }
        return result;
    }

    /**
     * Sesi dengan paling banyak 3 jadwal: ruangan dipakai sesuai urutan yang diberikan (dari pemanggil: kapasitas
     * terbesar dulu) sampai kursinya cukup, lalu tiap jadwal dibagi ke ruangan-ruangan itu sebanding kapasitasnya
     * (metode sisa terbesar). Jumlah per ruangan tidak pernah melebihi kapasitas.
     * Contoh 34 + 25 + 25 siswa di ruangan 35, 30, 25: tiap ruangan berisi ketiga jadwal, bukan satu ruangan satu kelas.
     */
    static List<RoomAssignment> mixProportional(List<Group> groups, List<Room> rooms) {
        int total = groups.stream().mapToInt(g -> g.getStudents().size()).sum();
        List<Room> used = new ArrayList<>();
        int capacity = 0;
        for (Room room : rooms) {
            if (capacity >= total) {
                break;
            }
            used.add(room);
            capacity += room.getCapacity();
        }

        // 1. bagian bulat ke bawah untuk semua jadwal dulu (jumlahnya per ruangan tidak mungkin melebihi kapasitas)
        int[][] count = new int[groups.size()][used.size()];
        double[][] exact = new double[groups.size()][used.size()];
        int[] filled = new int[used.size()];
        for (int g = 0; g < groups.size(); g++) {
            int size = groups.get(g).getStudents().size();
            for (int r = 0; r < used.size(); r++) {
                exact[g][r] = (double) size * used.get(r).getCapacity() / capacity;
                count[g][r] = (int) Math.floor(exact[g][r]);
                filled[r] += count[g][r];
            }
        }
        // 2. sisa siswa tiap jadwal ke ruangan dengan pecahan terbesar yang masih punya kursi kosong
        for (int g = 0; g < groups.size(); g++) {
            int assigned = 0;
            for (int r = 0; r < used.size(); r++) {
                assigned += count[g][r];
            }
            while (assigned < groups.get(g).getStudents().size()) {
                int best = -1;
                for (int r = 0; r < used.size(); r++) {
                    if (filled[r] >= used.get(r).getCapacity()) {
                        continue;
                    }
                    if (best < 0 || exact[g][r] - count[g][r] > exact[g][best] - count[g][best]) {
                        best = r;
                    }
                }
                count[g][best]++;
                filled[best]++;
                assigned++;
            }
        }

        List<RoomAssignment> result = new ArrayList<>();
        for (int r = 0; r < used.size(); r++) {
            result.add(new RoomAssignment(used.get(r)));
        }
        for (int g = 0; g < groups.size(); g++) {
            // siswa diambil berurutan nama: ruangan pertama mendapat nama-nama awal
            Deque<Student> queue = new ArrayDeque<>(groups.get(g).getStudents());
            for (int r = 0; r < used.size(); r++) {
                RoomAssignment assignment = result.get(r);
                for (int i = 0; i < count[g][r]; i++) {
                    assignment.getSeats().add(new SeatAssignment(assignment.getFilled() + 1, queue.poll(),
                            groups.get(g).getSchedule()));
                }
            }
        }
        result.removeIf(assignment -> assignment.getFilled() == 0);
        return result;
    }

    /**
     * Susun ulang kursi satu ruangan. Siswa dikelompokkan per jadwal (urutan nama di dalam jadwal tidak berubah),
     * lalu urutan jadwal per kursi diambil dari {@link #spreadOrder}.
     */
    public static void arrangeSeats(RoomAssignment assignment) {
        Map<Integer, List<SeatAssignment>> bySchedule = new LinkedHashMap<>();
        Map<Integer, ExamSchedule> schedules = new LinkedHashMap<>();
        for (SeatAssignment seat : assignment.getSeats()) {
            int id = seat.getSchedule().getId();
            bySchedule.computeIfAbsent(id, k -> new ArrayList<>()).add(seat);
            schedules.putIfAbsent(id, seat.getSchedule());
        }
        // urutan jadwal untuk kursi pertama & saat seri: tingkat dulu (X lalu XI) supaya beda tingkat selang-seling
        List<Integer> ids = new ArrayList<>(bySchedule.keySet());
        ids.sort(Comparator.comparing((Integer id) -> gradeOf(schedules.get(id))).thenComparingInt(id -> id));

        List<Integer> sizes = new ArrayList<>();
        for (Integer id : ids) {
            sizes.add(bySchedule.get(id).size());
        }

        List<SeatAssignment> arranged = new ArrayList<>();
        int[] taken = new int[ids.size()];
        for (int groupIndex : spreadOrder(sizes)) {
            SeatAssignment old = bySchedule.get(ids.get(groupIndex)).get(taken[groupIndex]++);
            arranged.add(new SeatAssignment(arranged.size() + 1, old.getStudent(), old.getSchedule()));
        }
        assignment.getSeats().clear();
        assignment.getSeats().addAll(arranged);
    }

    private static String gradeOf(ExamSchedule schedule) {
        return schedule.getClassroom() != null && schedule.getClassroom().getGrade() != null
                ? schedule.getClassroom().getGrade() : "";
    }

    /**
     * Urutan kelompok untuk tiap kursi (isi = indeks kelompok), dengan sizes = jumlah siswa per kelompok.
     * - Bila bisa (kelompok terbesar tidak lebih dari separuh kursi, dibulatkan ke atas): tidak ada dua kursi
     *   berurutan dari kelompok yang sama, dan tiap kelompok tersebar rata sepanjang ruangan.
     * - Bila tidak bisa (satu kelompok lebih dari separuh): kursi bersebelahan sekelompok tidak terhindarkan,
     *   jadi kelompok kecil disebar rata di antara kelompok besar (bukan menumpuk sisa kelompok besar di akhir).
     */
    static List<Integer> spreadOrder(List<Integer> sizes) {
        int n = sizes.size();
        int total = 0;
        int max = 0;
        for (int size : sizes) {
            total += size;
            max = Math.max(max, size);
        }

        List<Integer> order = new ArrayList<>(total);
        int[] taken = new int[n];

        if (max * 2 > total + 1) {
            // Satu kelompok lebih dari separuh: kelompok lain dipakai sebagai "penyekat". Kelompok besar dibagi
            // serata mungkin ke (jumlah penyekat + 1) celah, dan penyekat diurutkan tanpa sebelahan sejenis bila bisa.
            int big = sizes.indexOf(max);
            List<Integer> others = new ArrayList<>(sizes);
            others.set(big, 0);
            List<Integer> separators = spreadOrder(others);
            int slots = separators.size() + 1;
            for (int slot = 0; slot < slots; slot++) {
                int run = max * (slot + 1) / slots - max * slot / slots;
                for (int i = 0; i < run; i++) {
                    order.add(big);
                }
                if (slot < separators.size()) {
                    order.add(separators.get(slot));
                }
            }
            return order;
        }

        int previous = -1;
        for (int left = total; left > 0; left--) {
            int best = -1;
            for (int g = 0; g < n; g++) {
                int rest = sizes.get(g) - taken[g];
                if (rest == 0 || g == previous) {
                    continue;
                }
                // kelompok yang sisanya sudah lebih dari separuh kursi tersisa wajib duduk sekarang,
                // kalau tidak nanti pasti ada dua kursi berurutan sekelompok
                if (rest * 2 > left) {
                    best = g;
                    break;
                }
                if (best < 0 || idealPosition(taken, sizes, g) < idealPosition(taken, sizes, best)) {
                    best = g;
                }
            }
            if (best < 0) {
                best = previous;   // tidak terjadi bila syarat di atas terpenuhi; pengaman saja
            }
            order.add(best);
            taken[best]++;
            previous = best;
        }
        return order;
    }

    private static double idealPosition(int[] taken, List<Integer> sizes, int group) {
        return (taken[group] + 0.5) / sizes.get(group);
    }

    private static List<Group> activeGroups(Map<Group, Deque<Student>> remaining) {
        List<Group> active = new ArrayList<>();
        remaining.forEach((group, queue) -> {
            if (!queue.isEmpty()) {
                active.add(group);
            }
        });
        return active;
    }

    private static final int TINY_REMNANT = 6;   // sisa kelas di bawah ini dianggap "serpihan" yang merepotkan

    private static void fillRoom(RoomAssignment assignment, List<Group> active, Map<Group, Deque<Student>> remaining) {
        int capacity = assignment.getRoom().getCapacity();

        // Coba semua kombinasi 2-3 kelas (atau 1 kelas jika tinggal satu), pilih yang nilainya terbaik.
        List<Group> chosen = null;
        Map<Group, Integer> quota = null;
        double bestScore = Double.MAX_VALUE;

        for (List<Group> candidate : combinations(active, Math.min(3, active.size()))) {
            Map<Group, Integer> share = shareCapacity(candidate, remaining, capacity);
            double score = score(candidate, share, remaining, capacity, active);
            if (score < bestScore) {
                bestScore = score;
                chosen = candidate;
                quota = share;
            }
        }

        // kelompok diurutkan per tingkat agar tingkat yang berbeda selang-seling di kursi
        chosen = new ArrayList<>(chosen);
        chosen.sort(Comparator.comparing(Group::grade).thenComparingInt(g -> g.getSchedule().getId()));
        Map<Group, Deque<Student>> taken = new LinkedHashMap<>();
        for (Group group : chosen) {
            Deque<Student> mine = new ArrayDeque<>();
            Deque<Student> queue = remaining.get(group);
            for (int i = 0; i < quota.get(group) && !queue.isEmpty(); i++) {
                mine.add(queue.poll());
            }
            taken.put(group, mine);
        }

        boolean any = true;
        while (any) {
            any = false;
            for (Map.Entry<Group, Deque<Student>> entry : taken.entrySet()) {
                Student student = entry.getValue().poll();
                if (student != null) {
                    assignment.getSeats().add(new SeatAssignment(assignment.getFilled() + 1, student, entry.getKey().getSchedule()));
                    any = true;
                }
            }
        }
    }

    // Semua kombinasi 2 sampai maxSize kelas; jika hanya ada 1 kelas aktif, kombinasinya kelas itu saja
    private static List<List<Group>> combinations(List<Group> active, int maxSize) {
        List<List<Group>> result = new ArrayList<>();
        if (active.size() == 1) {
            result.add(new ArrayList<>(active));
            return result;
        }
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                result.add(new ArrayList<>(List.of(active.get(i), active.get(j))));
                if (maxSize >= 3) {
                    for (int k = j + 1; k < active.size(); k++) {
                        result.add(new ArrayList<>(List.of(active.get(i), active.get(j), active.get(k))));
                    }
                }
            }
        }
        return result;
    }

    // Nilai kombinasi (makin kecil makin baik): hindari serpihan, kursi kosong, kelas yang terlalu terpecah,
    // dan kombinasi yang tidak bercampur tingkat; sedikit pilih yang seimbang.
    private static double score(List<Group> candidate, Map<Group, Integer> quota,
                                Map<Group, Deque<Student>> remaining, int capacity, List<Group> active) {
        double score = 0;
        int used = 0;
        int minQuota = Integer.MAX_VALUE;
        int maxQuota = 0;

        for (Group group : candidate) {
            int give = quota.get(group);
            int left = remaining.get(group).size() - give;
            used += give;
            minQuota = Math.min(minQuota, give);
            maxQuota = Math.max(maxQuota, give);
            if (left > 0) {
                score += 3;                              // kelas ini masih harus dipecah ke ruangan lain
                if (left < TINY_REMNANT) {
                    score += 40;                         // dan sisanya serpihan kecil
                }
            }
        }

        score += (capacity - used) * 2.0;                // kursi kosong
        if (candidate.size() == 3) {
            score += 2;                                  // utamakan 2 kelas per ruangan
        }

        boolean mixedGrade = candidate.stream().map(Group::grade).distinct().count() > 1;
        boolean mixPossible = active.stream().map(Group::grade).distinct().count() > 1;
        if (mixPossible && !mixedGrade) {
            score += 25;                                 // tingkat berbeda seharusnya bercampur
        }

        long distinctSubjects = candidate.stream().map(Group::subjectId).distinct().count();
        score += (candidate.size() - distinctSubjects) * 4.0;   // mapel sama dalam satu ruangan dihindari

        score += (maxQuota - minQuota) * 0.1;            // seimbang antar kelas

        // Jaga agar semua tingkat habis bersamaan: tingkat yang sisanya terbanyak perlu cepat dikurangi,
        // supaya ruangan-ruangan terakhir tidak berisi satu tingkat saja.
        Map<String, Integer> gradeLeft = new LinkedHashMap<>();
        for (Group group : active) {
            int left = remaining.get(group).size() - (quota.containsKey(group) ? quota.get(group) : 0);
            gradeLeft.merge(group.grade(), left, Integer::sum);
        }
        if (gradeLeft.size() > 1) {
            int max = gradeLeft.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int min = gradeLeft.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            score += (max - min) * 0.3;
        }
        return score;
    }

    // Bagi kapasitas serata mungkin; kelompok yang sisanya lebih sedikit dari jatah diambil semuanya
    private static Map<Group, Integer> shareCapacity(List<Group> chosen, Map<Group, Deque<Student>> remaining, int capacity) {
        List<Group> order = new ArrayList<>(chosen);
        order.sort(Comparator.comparingInt(g -> remaining.get(g).size()));

        Map<Group, Integer> quota = new LinkedHashMap<>();
        int capacityLeft = capacity;
        int groupsLeft = order.size();
        for (int i = 0; i < order.size(); i++) {
            Group group = order.get(i);
            int need = remaining.get(group).size();
            int share = capacityLeft / groupsLeft;
            if (need <= share) {
                quota.put(group, need);
                capacityLeft -= need;
                groupsLeft--;
            } else {
                int extra = capacityLeft - share * groupsLeft;
                for (int j = i; j < order.size(); j++) {
                    Group other = order.get(j);
                    int plus = extra > 0 ? 1 : 0;
                    if (extra > 0) {
                        extra--;
                    }
                    quota.put(other, Math.min(remaining.get(other).size(), share + plus));
                }
                break;
            }
        }
        return quota;
    }
}