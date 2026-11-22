# Database `penjadwalan_ujian_db`

## Instalasi baru (komputer/server yang belum punya database)

1. Buat database kosong: `CREATE DATABASE penjadwalan_ujian_db;`
2. Impor `schema.sql`. File ini sudah berisi struktur terbaru (semua migrasi di bawah sudah termasuk).
3. Isi data awal lewat aplikasi (atau impor dump data bila ada), lalu buat akun admin.

`schema.sql` dan semua migrasi diuji di MariaDB 10.11. Untuk MariaDB 10.4 (XAMPP), coba dulu di salinan database.

## Memperbarui database lama (yang dibuat dari dump sebelum 21 Nov 2026)

Backup dulu lewat phpMyAdmin > Export, lalu jalankan file migrasi **berurutan** (yang sudah pernah dijalankan tidak perlu diulang):

| No | File | Isi singkat | Boleh diulang? |
|---|---|---|---|
| 1 | `migrasi_kode_guru.sql` | kolom kode guru pada pengampu | ya |
| 2 | `migrasi_nip_opsional.sql` | NIP guru boleh kosong, tetap unik bila diisi | ya |
| 3 | `migrasi_penempatan_ruangan.sql` | ruangan & pengawas pindah ke Penempatan Ruangan, FK RESTRICT | **tidak, sekali saja** |
| 4 | `migrasi_akun_guru.sql` | guru yang punya akun tidak bisa dihapus, aturan role akun | ya |
| 5 | `migrasi_kursi_penempatan.sql` | No. Kursi hanya dari Penempatan Ruangan | ya |
| 6 | `migrasi_hapus_photo_path.sql` | hapus kolom `photo_path` (foto ada di `photo_data`) | ya |
| 7 | `migrasi_status_administrasi.sql` | status administrasi siswa (Lunas / Belum Lunas) | ya |

Catatan:

- Urutan penting. Nomor 5 harus setelah nomor 3, karena nomor 3 membuat ulang relasi ruangan
  pada `trx_peserta` dan nomor 5 mengubahnya lagi.
- Nomor 3 bila tidak sengaja dijalankan dua kali langsung berhenti di langkah pertama
  (tabel `bak_trx_jadwal_ruangan` sudah ada), jadi tidak ada data yang berubah.
- Nomor 4 diawali query cek. Bila query itu menampilkan baris, perbaiki akun tersebut di menu
  User Management lalu jalankan ulang file nomor 4.
- Dump dari phpMyAdmin tidak menyimpan aturan CHECK (tabel `users` dan `trx_peserta`). Bila database dipulihkan
  dari dump, jalankan lagi nomor 4 dan 5, atau buat struktur dari `schema.sql` lalu impor datanya saja.
- Tabel `bak_trx_jadwal_ruangan` hanya cadangan dari migrasi nomor 3. Boleh dihapus setelah data
  penempatan dipastikan benar.
