-- Menambah kode guru pada pengampu (angka di belakang nama mapel di jadwal, mis. DDPK TKJ (1)).
-- Aman dijalankan berulang di MariaDB: kalau kolomnya sudah ada, tidak terjadi apa-apa.
ALTER TABLE mst_pengampu ADD COLUMN IF NOT EXISTS kode_guru int(11) DEFAULT NULL AFTER id_kelas;