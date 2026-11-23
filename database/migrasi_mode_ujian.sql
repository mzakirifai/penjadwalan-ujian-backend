-- Mode ujian per jadwal: Offline (di ruangan, ikut Penempatan Ruangan) atau Online (tanpa ruangan & pengawas).
-- Jadwal yang sudah ada otomatis menjadi Offline (perilaku lama). Aman dijalankan berulang.
ALTER TABLE trx_jadwal
    ADD COLUMN IF NOT EXISTS mode_ujian ENUM('Offline','Online') NOT NULL DEFAULT 'Offline' AFTER tahun_akademik;
