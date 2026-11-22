-- NIP guru boleh kosong (banyak guru honorer belum punya NIP), tetapi tetap unik bila diisi.
-- UNIQUE KEY `nip` tetap dipakai: MariaDB mengizinkan banyak baris NULL pada kolom UNIQUE.
-- Urutan penting: kolom dibuat boleh NULL dulu, baru NIP kosong ('') diubah menjadi NULL.
-- Aman dijalankan berulang.
ALTER TABLE mst_guru MODIFY nip VARCHAR(20) DEFAULT NULL;
UPDATE mst_guru SET nip = NULL WHERE nip IS NOT NULL AND TRIM(nip) = '';
