-- Kolom photo_path sudah tidak dipakai: foto profil disimpan sebagai data gambar di kolom photo_data,
-- sehingga tampil di komputer mana pun yang terhubung ke database yang sama.
-- photo_path hanya berisi path lama di laptop masing-masing (mis. C:\Users\...), yang tidak berlaku di komputer lain.
-- Aman dijalankan berulang.
ALTER TABLE users DROP COLUMN IF EXISTS photo_path;
