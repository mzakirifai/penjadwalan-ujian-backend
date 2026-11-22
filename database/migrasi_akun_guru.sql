-- Akun login role "guru" harus selalu terhubung ke satu data guru.
-- Sebelumnya FK users -> mst_guru memakai ON DELETE SET NULL: menghapus guru membuat akunnya
-- menjadi role "guru" tanpa id_guru (akun yatim). Sekarang penghapusan guru yang masih punya akun ditolak.
-- Aman dijalankan berulang.

-- 0. Cek dulu: query ini harus kosong. Bila ada baris, perbaiki akun tersebut di menu User Management
--    (hubungkan ke guru atau ubah role menjadi admin), lalu jalankan ulang file ini.
SELECT user_id, username, role, id_guru FROM users
WHERE (role = 'guru' AND id_guru IS NULL) OR (role = 'admin' AND id_guru IS NOT NULL);

-- 1. FK: hapus guru yang masih punya akun ditolak (RESTRICT), bukan dikosongkan (SET NULL).
--    ON UPDATE juga RESTRICT: id_guru auto increment tidak pernah diubah, dan MariaDB 10.5+ menolak
--    CHECK di langkah 2 bila kolomnya memakai aksi CASCADE/SET NULL.
ALTER TABLE users DROP FOREIGN KEY IF EXISTS users_ibfk_1;
ALTER TABLE users ADD CONSTRAINT users_ibfk_1 FOREIGN KEY (id_guru)
    REFERENCES mst_guru (id_guru) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- 2. Aturan role: admin tanpa guru, guru wajib punya guru (sama dengan validasi di aplikasi)
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role_guru;
ALTER TABLE users ADD CONSTRAINT chk_users_role_guru
    CHECK ((role = 'admin' AND id_guru IS NULL) OR (role = 'guru' AND id_guru IS NOT NULL));
