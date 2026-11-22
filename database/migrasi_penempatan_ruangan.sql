-- Ruangan & pengawas tidak lagi melekat pada jadwal (trx_jadwal), tetapi diatur per sesi lewat
-- menu Penempatan Ruangan (kursi peserta di trx_peserta, pengawas di trx_pengawas_ruangan).
-- JALANKAN SEKALI SAJA. Bila dijalankan ulang, langkah 0 gagal (tabel cadangan sudah ada) dan
-- file berhenti di situ tanpa mengubah apa pun.

-- 0. Cadangkan ruangan & pengawas lama dari jadwal
CREATE TABLE bak_trx_jadwal_ruangan AS
SELECT id_ujian, kode_ujian, tanggal, jam_mulai, jam_selesai, id_kelas, id_ruangan, id_guru,
       NOW() AS dicadangkan_pada
FROM trx_jadwal
WHERE id_ruangan IS NOT NULL OR id_guru IS NOT NULL;

-- 1. Kursi peserta yang sudah bernomor tetapi belum punya ruangan: pakai ruangan lama dari jadwalnya
UPDATE trx_peserta ep
JOIN trx_jadwal ej ON ej.id_ujian = ep.id_ujian
SET ep.id_ruangan = ej.id_ruangan
WHERE ep.id_ruangan IS NULL AND ep.no_kursi IS NOT NULL AND ej.id_ruangan IS NOT NULL;

-- 2. Pengawas lama menjadi pengawas ruangan pada sesi itu (bila ruangannya berisi kursi peserta jadwal tsb)
INSERT IGNORE INTO trx_pengawas_ruangan (tanggal, jam_mulai, jam_selesai, id_ruangan, id_guru, diubah_oleh)
SELECT ej.tanggal, ej.jam_mulai, ej.jam_selesai, ej.id_ruangan, ej.id_guru, 'migrasi'
FROM trx_jadwal ej
WHERE ej.id_ruangan IS NOT NULL AND ej.id_guru IS NOT NULL
  AND EXISTS (SELECT 1 FROM trx_peserta ep WHERE ep.id_ujian = ej.id_ujian AND ep.id_ruangan = ej.id_ruangan);

-- 3. Hapus kolom ruangan & pengawas dari jadwal
ALTER TABLE trx_jadwal DROP FOREIGN KEY trx_jadwal_ibfk_3;
ALTER TABLE trx_jadwal DROP FOREIGN KEY trx_jadwal_ibfk_4;
ALTER TABLE trx_jadwal DROP COLUMN id_ruangan, DROP COLUMN id_guru;

-- 4. Data master yang masih dipakai tidak boleh terhapus diam-diam (ON DELETE RESTRICT):
--    aplikasi menampilkan pesan yang jelas bila penghapusan ditolak.
ALTER TABLE trx_jadwal DROP FOREIGN KEY trx_jadwal_ibfk_1;
ALTER TABLE trx_jadwal DROP FOREIGN KEY trx_jadwal_ibfk_2;
ALTER TABLE trx_jadwal
    ADD CONSTRAINT trx_jadwal_ibfk_1 FOREIGN KEY (id_mapel) REFERENCES mst_mapel (id_mapel) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT trx_jadwal_ibfk_2 FOREIGN KEY (id_kelas) REFERENCES mst_kelas (id_kelas) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE trx_pengawas_ruangan DROP FOREIGN KEY trx_pengawas_ruangan_ibfk_1;
ALTER TABLE trx_pengawas_ruangan DROP FOREIGN KEY trx_pengawas_ruangan_ibfk_2;
ALTER TABLE trx_pengawas_ruangan
    ADD CONSTRAINT trx_pengawas_ruangan_ibfk_1 FOREIGN KEY (id_ruangan) REFERENCES mst_ruangan (id_ruangan) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT trx_pengawas_ruangan_ibfk_2 FOREIGN KEY (id_guru) REFERENCES mst_guru (id_guru) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE trx_peserta DROP FOREIGN KEY trx_peserta_ibfk_3;
ALTER TABLE trx_peserta
    ADD CONSTRAINT trx_peserta_ibfk_3 FOREIGN KEY (id_ruangan) REFERENCES mst_ruangan (id_ruangan) ON DELETE RESTRICT ON UPDATE CASCADE;
