package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.TeachingAssignmentDAO;
import co.id.model.Classroom;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.TeachingAssignment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TeachingAssignmentDAOImpl extends DatabaseConfiguration implements TeachingAssignmentDAO {

    private static final String SELECT_JOIN =
            "SELECT p.*, g.nip, g.nama_guru, m.kode_mapel, m.nama_mapel, k.kode_kelas, k.nama_kelas "
            + "FROM mst_pengampu p "
            + "JOIN mst_guru g ON p.id_guru = g.id_guru "
            + "JOIN mst_mapel m ON p.id_mapel = m.id_mapel "
            + "JOIN mst_kelas k ON p.id_kelas = k.id_kelas ";

    private static final String ORDER_BY = "ORDER BY g.nama_guru, k.nama_kelas, m.nama_mapel";

    @Override
    public List<TeachingAssignment> getAllTeachingAssignments() {
        return query(SELECT_JOIN + ORDER_BY);
    }

    @Override
    public List<TeachingAssignment> getTeachingAssignmentBy(String keyword) {
        String like = "%" + keyword + "%";
        return query(SELECT_JOIN + "WHERE g.nama_guru LIKE ? OR g.nip LIKE ? OR m.nama_mapel LIKE ? OR k.nama_kelas LIKE ? " + ORDER_BY,
                like, like, like, like);
    }

    @Override
    public List<TeachingAssignment> getTeachingAssignments(int page, int size) {
        return query(SELECT_JOIN + ORDER_BY + " LIMIT ? OFFSET ?", size, (page - 1) * size);
    }

    @Override
    public List<TeachingAssignment> getByTeacher(int teacherId) {
        return query(SELECT_JOIN + "WHERE p.id_guru = ? " + ORDER_BY, teacherId);
    }

    @Override
    public int countTeachingAssignments() {
        return count("SELECT COUNT(*) FROM mst_pengampu");
    }

    @Override
    public TeachingAssignment getById(int id) {
        List<TeachingAssignment> result = query(SELECT_JOIN + "WHERE p.id_pengampu = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public void saveOrUpdate(TeachingAssignment ta) {
        String sqlInsert = "INSERT INTO mst_pengampu(id_guru, id_mapel, id_kelas, diubah_oleh) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_pengampu SET id_guru = ?, id_mapel = ?, id_kelas = ?, diubah_oleh = ? WHERE id_pengampu = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(ta.getId() == 0 ? sqlInsert : sqlUpdate)) {

            ps.setInt(1, ta.getTeacher().getId());
            ps.setInt(2, ta.getSubject().getId());
            ps.setInt(3, ta.getClassroom().getId());
            ps.setString(4, ta.getUpdatedBy());
            if (ta.getId() != 0) {
                ps.setInt(5, ta.getId());
            }
            ps.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM mst_pengampu WHERE id_pengampu = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean isAssigned(int teacherId, int subjectId, int classroomId) {
        return count("SELECT COUNT(*) FROM mst_pengampu WHERE id_guru = ? AND id_mapel = ? AND id_kelas = ?",
                teacherId, subjectId, classroomId) > 0;
    }

    @Override
    public boolean isDuplicate(int teacherId, int subjectId, int classroomId, int excludeId) {
        return count("SELECT COUNT(*) FROM mst_pengampu WHERE id_guru = ? AND id_mapel = ? AND id_kelas = ? AND id_pengampu <> ?",
                teacherId, subjectId, classroomId, excludeId) > 0;
    }

    // ---------- helper ----------

    private List<TeachingAssignment> query(String sql, Object... params) {
        List<TeachingAssignment> list = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return list;
    }

    private int count(String sql, Object... params) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    private void bind(PreparedStatement ps, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Integer value) {
                ps.setInt(i + 1, value);
            } else {
                ps.setString(i + 1, String.valueOf(params[i]));
            }
        }
    }

    private TeachingAssignment map(ResultSet rs) throws Exception {
        TeachingAssignment ta = new TeachingAssignment();
        ta.setId(rs.getInt("id_pengampu"));

        Teacher teacher = new Teacher();
        teacher.setId(rs.getInt("id_guru"));
        teacher.setNip(rs.getString("nip"));
        teacher.setName(rs.getString("nama_guru"));
        ta.setTeacher(teacher);

        Subject subject = new Subject();
        subject.setId(rs.getInt("id_mapel"));
        subject.setCode(rs.getString("kode_mapel"));
        subject.setName(rs.getString("nama_mapel"));
        ta.setSubject(subject);

        Classroom classroom = new Classroom();
        classroom.setId(rs.getInt("id_kelas"));
        classroom.setCode(rs.getString("kode_kelas"));
        classroom.setName(rs.getString("nama_kelas"));
        ta.setClassroom(classroom);

        Timestamp createdAt = rs.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            ta.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            ta.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        ta.setUpdatedBy(rs.getString("diubah_oleh"));
        return ta;
    }
}