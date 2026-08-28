package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.TeacherDAO;
import co.id.model.Teacher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAOImpl extends DatabaseConfiguration implements TeacherDAO{

    @Override
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM mst_guru";

        try(Connection connection = getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)){

            while(resultSet.next()){
                teachers.add(mapResultSetToTeacher(resultSet));
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }

        return teachers;
    }

    @Override
    public List<Teacher> getTeacherBy(String keyword) {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM mst_guru WHERE nama_guru LIKE ? OR nip LIKE ?";

        try(Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)){

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()){
                while(resultSet.next()){
                    teachers.add(mapResultSetToTeacher(resultSet));
                }
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }

        return teachers;
    }

    @Override
    public List<Teacher> getTeachers(int page, int size) {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM mst_guru ORDER BY id_guru LIMIT ? OFFSET ?";

        try(Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)){

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try(ResultSet resultSet = preparedStatement.executeQuery()){
                while(resultSet.next()){
                    teachers.add(mapResultSetToTeacher(resultSet));
                }
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }

        return teachers;
    }

    @Override
    public int countTeachers() {
        String sql = "SELECT COUNT(*) AS total FROM mst_guru";

        try(Connection connection = getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)){

            if(resultSet.next()){
                return resultSet.getInt("total");
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }

        return 0;
    }

    @Override
    public Teacher getById(int id) {
        String sql = "SELECT * FROM mst_guru WHERE id_guru = ?";

        try(Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)){

            preparedStatement.setInt(1, id);

            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    return mapResultSetToTeacher(resultSet);
                }
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(Teacher teacher) {
        String sqlInsert = "INSERT INTO mst_guru(nip, nama_guru, jenis_kelamin, nomor_hp, email, alamat, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_guru SET nip = ?, nama_guru = ?, jenis_kelamin = ?, nomor_hp = ?, email = ?, alamat = ?, diubah_oleh = ? WHERE id_guru = ?";

        try (Connection connection = getConnection()){
            if (teacher.getId() == 0){
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert)){
                    preparedStatement.setString(1, teacher.getNip());
                    preparedStatement.setString(2, teacher.getName());
                    preparedStatement.setString(3, teacher.getGender());
                    preparedStatement.setString(4, teacher.getPhoneNumber());
                    preparedStatement.setString(5, teacher.getEmail());
                    preparedStatement.setString(6, teacher.getAddress());
                    preparedStatement.setString(7, teacher.getUpdatedBy());
                    
                    preparedStatement.executeUpdate();
                }
            }else{
                try(PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)){
                    preparedStatement.setString(1, teacher.getNip());
                    preparedStatement.setString(2, teacher.getName());
                    preparedStatement.setString(3, teacher.getGender());
                    preparedStatement.setString(4, teacher.getPhoneNumber());
                    preparedStatement.setString(5, teacher.getEmail());
                    preparedStatement.setString(6, teacher.getAddress());
                    preparedStatement.setString(7, teacher.getUpdatedBy());
                    preparedStatement.setInt(8, teacher.getId());
                    
                    
                    preparedStatement.executeUpdate();
                }
            }
        }catch(Exception exception){
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_guru WHERE id_guru = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    // Helper method untuk mapping ResultSet ke object Teacher
    private Teacher mapResultSetToTeacher(ResultSet resultSet) throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(resultSet.getInt("id_guru"));
        teacher.setNip(resultSet.getString("nip"));
        teacher.setName(resultSet.getString("nama_guru"));
        teacher.setGender(resultSet.getString("jenis_kelamin"));
        teacher.setPhoneNumber(resultSet.getString("nomor_hp"));
        teacher.setEmail(resultSet.getString("email"));
        teacher.setAddress(resultSet.getString("alamat"));
        
        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            teacher.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            teacher.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        teacher.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return teacher;
    }
}
