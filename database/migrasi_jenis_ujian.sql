-- Jenis ujian jadi data master (menu Master > Jenis Ujian), tidak lagi ditulis di kode program.
--  * Tabel mst_jenis_ujian diisi 5 jenis bawaan (UTS, UAS, AAS, UKK, PRAKTIK) + jenis lain yang sudah
--    terlanjur dipakai di jadwal, supaya tidak ada jadwal yang kehilangan jenisnya.
--  * trx_jadwal.jenis_ujian dihubungkan ke kode jenis: ganti kode -> semua jadwal ikut berubah (CASCADE),
--    jenis yang masih dipakai jadwal tidak bisa dihapus (RESTRICT).
-- Aman dijalankan berulang.
CREATE TABLE IF NOT EXISTS `mst_jenis_ujian` (
  `id_jenis_ujian` int(11) NOT NULL AUTO_INCREMENT,
  `kode` varchar(20) NOT NULL,
  `nama_lengkap` varchar(100) NOT NULL,
  `aktif` tinyint(1) NOT NULL DEFAULT 1,
  `urutan` int(11) NOT NULL DEFAULT 0,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_jenis_ujian`),
  UNIQUE KEY `kode` (`kode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO mst_jenis_ujian (kode, nama_lengkap, urutan, diubah_oleh) VALUES
    ('UTS', 'Ujian Tengah Semester', 1, 'migrasi'),
    ('UAS', 'Ujian Akhir Semester', 2, 'migrasi'),
    ('AAS', 'Asesmen Akhir Semester', 3, 'migrasi'),
    ('UKK', 'Ujian Kenaikan Kelas', 4, 'migrasi'),
    ('PRAKTIK', 'Ujian Praktik', 5, 'migrasi');

INSERT IGNORE INTO mst_jenis_ujian (kode, nama_lengkap, urutan, diubah_oleh)
SELECT DISTINCT j.jenis_ujian, j.jenis_ujian, 99, 'migrasi'
FROM trx_jadwal j
WHERE j.jenis_ujian IS NOT NULL AND j.jenis_ujian <> ''
  AND NOT EXISTS (SELECT 1 FROM mst_jenis_ujian m WHERE m.kode = j.jenis_ujian);

ALTER TABLE trx_jadwal
    ADD CONSTRAINT `fk_jadwal_jenis_ujian` FOREIGN KEY IF NOT EXISTS (`jenis_ujian`)
        REFERENCES `mst_jenis_ujian` (`kode`) ON UPDATE CASCADE ON DELETE RESTRICT;
