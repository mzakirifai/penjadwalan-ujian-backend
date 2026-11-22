-- No. Kursi sekarang hanya diatur oleh menu Penempatan Ruangan.
-- Sebelumnya layar Peserta membuat K01, K02, ... saat pendaftaran, tanpa ruangan. Kursi tanpa ruangan
-- tidak dijaga UNIQUE KEY uniq_ujian_ruangan_kursi (NULL dianggap berbeda), sehingga bisa dobel.
-- Aman dijalankan berulang.

-- 1. Kursi yang belum punya ruangan (belum ditempatkan) dikosongkan
UPDATE trx_peserta SET no_kursi = NULL WHERE id_ruangan IS NULL AND no_kursi IS NOT NULL;

-- 2. FK ruangan: ON UPDATE juga RESTRICT (id_ruangan auto increment tidak pernah diubah);
--    MariaDB 10.5+ menolak CHECK di langkah 3 bila kolomnya memakai aksi CASCADE/SET NULL.
ALTER TABLE trx_peserta DROP FOREIGN KEY IF EXISTS trx_peserta_ibfk_3;
ALTER TABLE trx_peserta ADD CONSTRAINT trx_peserta_ibfk_3 FOREIGN KEY (id_ruangan)
    REFERENCES mst_ruangan (id_ruangan) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- 3. No. Kursi hanya boleh ada bila peserta sudah punya ruangan
ALTER TABLE trx_peserta DROP CONSTRAINT IF EXISTS chk_peserta_kursi_ruangan;
ALTER TABLE trx_peserta ADD CONSTRAINT chk_peserta_kursi_ruangan
    CHECK (no_kursi IS NULL OR id_ruangan IS NOT NULL);
