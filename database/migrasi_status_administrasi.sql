-- Status administrasi (keuangan) siswa, dipakai saat memilih peserta ujian:
-- siswa "Belum Lunas" otomatis tidak dicentang, tetapi admin tetap bisa mencentangnya (dispensasi).
-- Semua siswa yang sudah ada diberi status "Lunas" supaya tidak ada yang tiba-tiba tertahan;
-- bagian keuangan/TU memperbarui statusnya lewat halaman Siswa atau import Excel.
-- Aman dijalankan berulang.
ALTER TABLE mst_siswa
    ADD COLUMN IF NOT EXISTS status_administrasi ENUM('Lunas','Belum Lunas') NOT NULL DEFAULT 'Lunas' AFTER jenis_kelamin,
    ADD COLUMN IF NOT EXISTS keterangan_administrasi VARCHAR(255) DEFAULT NULL AFTER status_administrasi;
