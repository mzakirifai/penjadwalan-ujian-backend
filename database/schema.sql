-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: penjadwalan_ujian_db
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `mst_guru`
--

DROP TABLE IF EXISTS `mst_guru`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_guru` (
  `id_guru` int(11) NOT NULL AUTO_INCREMENT,
  `nip` varchar(20) NOT NULL,
  `nama_guru` varchar(100) NOT NULL,
  `jenis_kelamin` enum('Laki-laki','Perempuan') NOT NULL,
  `nomor_hp` varchar(15) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `alamat` text DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_guru`),
  UNIQUE KEY `nip` (`nip`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_jurusan`
--

DROP TABLE IF EXISTS `mst_jurusan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_jurusan` (
  `id_jurusan` int(11) NOT NULL AUTO_INCREMENT,
  `kode_jurusan` varchar(10) DEFAULT NULL,
  `nama_jurusan` varchar(100) NOT NULL,
  `singkatan` varchar(10) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_jurusan`),
  UNIQUE KEY `kode_jurusan` (`kode_jurusan`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_kelas`
--

DROP TABLE IF EXISTS `mst_kelas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_kelas` (
  `id_kelas` int(11) NOT NULL AUTO_INCREMENT,
  `kode_kelas` varchar(10) DEFAULT NULL,
  `nama_kelas` varchar(100) NOT NULL,
  `tingkat` enum('X','XI','XII') DEFAULT NULL,
  `id_jurusan` int(11) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_kelas`),
  UNIQUE KEY `kode_kelas` (`kode_kelas`),
  KEY `id_jurusan` (`id_jurusan`),
  CONSTRAINT `mst_kelas_ibfk_1` FOREIGN KEY (`id_jurusan`) REFERENCES `mst_jurusan` (`id_jurusan`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_mapel`
--

DROP TABLE IF EXISTS `mst_mapel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_mapel` (
  `id_mapel` int(11) NOT NULL AUTO_INCREMENT,
  `kode_mapel` varchar(10) DEFAULT NULL,
  `nama_mapel` varchar(100) NOT NULL,
  `id_jurusan` int(11) DEFAULT NULL,
  `tingkat` enum('X','XI','XII') DEFAULT NULL,
  `jenis` varchar(20) DEFAULT NULL,
  `kkm` int(11) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_mapel`),
  UNIQUE KEY `kode_mapel` (`kode_mapel`),
  KEY `id_jurusan` (`id_jurusan`),
  CONSTRAINT `mst_mapel_ibfk_1` FOREIGN KEY (`id_jurusan`) REFERENCES `mst_jurusan` (`id_jurusan`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_pengampu`
--

DROP TABLE IF EXISTS `mst_pengampu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_pengampu` (
  `id_pengampu` int(11) NOT NULL AUTO_INCREMENT,
  `id_guru` int(11) NOT NULL,
  `id_mapel` int(11) NOT NULL,
  `id_kelas` int(11) NOT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_pengampu`),
  UNIQUE KEY `uniq_guru_mapel_kelas` (`id_guru`,`id_mapel`,`id_kelas`),
  KEY `id_mapel` (`id_mapel`),
  KEY `id_kelas` (`id_kelas`),
  CONSTRAINT `mst_pengampu_ibfk_1` FOREIGN KEY (`id_guru`) REFERENCES `mst_guru` (`id_guru`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `mst_pengampu_ibfk_2` FOREIGN KEY (`id_mapel`) REFERENCES `mst_mapel` (`id_mapel`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `mst_pengampu_ibfk_3` FOREIGN KEY (`id_kelas`) REFERENCES `mst_kelas` (`id_kelas`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_ruangan`
--

DROP TABLE IF EXISTS `mst_ruangan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_ruangan` (
  `id_ruangan` int(11) NOT NULL AUTO_INCREMENT,
  `kode_ruangan` varchar(10) DEFAULT NULL,
  `nama_ruangan` varchar(100) NOT NULL,
  `lantai` int(11) DEFAULT NULL,
  `kapasitas` int(11) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_ruangan`),
  UNIQUE KEY `kode_ruangan` (`kode_ruangan`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mst_siswa`
--

DROP TABLE IF EXISTS `mst_siswa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mst_siswa` (
  `id_siswa` int(11) NOT NULL AUTO_INCREMENT,
  `nis` varchar(20) NOT NULL,
  `nisn` varchar(10) DEFAULT NULL,
  `nama_siswa` varchar(100) NOT NULL,
  `id_kelas` int(11) DEFAULT NULL,
  `jenis_kelamin` enum('Laki-laki','Perempuan') DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_siswa`),
  UNIQUE KEY `nis` (`nis`),
  UNIQUE KEY `uq_mst_siswa_nisn` (`nisn`),
  KEY `id_kelas` (`id_kelas`),
  CONSTRAINT `mst_siswa_ibfk_1` FOREIGN KEY (`id_kelas`) REFERENCES `mst_kelas` (`id_kelas`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pengaturan`
--

DROP TABLE IF EXISTS `pengaturan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pengaturan` (
  `id` int(11) NOT NULL,
  `nama_sekolah` varchar(150) NOT NULL,
  `alamat` varchar(255) DEFAULT NULL,
  `telepon` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `nama_kepala_sekolah` varchar(150) DEFAULT NULL,
  `nip_kepala_sekolah` varchar(50) DEFAULT NULL,
  `semester_aktif` varchar(20) DEFAULT NULL,
  `tahun_akademik_aktif` varchar(20) DEFAULT NULL,
  `tanggal_update` timestamp NULL DEFAULT NULL,
  `diubah_oleh` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trx_jadwal`
--

DROP TABLE IF EXISTS `trx_jadwal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trx_jadwal` (
  `id_ujian` int(11) NOT NULL AUTO_INCREMENT,
  `kode_ujian` varchar(10) DEFAULT NULL,
  `jenis_ujian` varchar(20) NOT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `tahun_akademik` varchar(10) DEFAULT NULL,
  `tanggal` date NOT NULL,
  `jam_mulai` time NOT NULL,
  `jam_selesai` time NOT NULL,
  `id_mapel` int(11) DEFAULT NULL,
  `id_kelas` int(11) DEFAULT NULL,
  `id_ruangan` int(11) DEFAULT NULL,
  `id_guru` int(11) DEFAULT NULL,
  `keterangan` text DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_ujian`),
  UNIQUE KEY `kode_ujian` (`kode_ujian`),
  KEY `id_mapel` (`id_mapel`),
  KEY `id_kelas` (`id_kelas`),
  KEY `id_ruangan` (`id_ruangan`),
  KEY `id_guru` (`id_guru`),
  CONSTRAINT `trx_jadwal_ibfk_1` FOREIGN KEY (`id_mapel`) REFERENCES `mst_mapel` (`id_mapel`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `trx_jadwal_ibfk_2` FOREIGN KEY (`id_kelas`) REFERENCES `mst_kelas` (`id_kelas`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `trx_jadwal_ibfk_3` FOREIGN KEY (`id_ruangan`) REFERENCES `mst_ruangan` (`id_ruangan`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `trx_jadwal_ibfk_4` FOREIGN KEY (`id_guru`) REFERENCES `mst_guru` (`id_guru`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trx_nilai`
--

DROP TABLE IF EXISTS `trx_nilai`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trx_nilai` (
  `id_nilai` int(11) NOT NULL AUTO_INCREMENT,
  `kode_nilai` varchar(35) DEFAULT NULL,
  `id_ujian` int(11) DEFAULT NULL,
  `id_siswa` int(11) DEFAULT NULL,
  `nilai` int(11) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_nilai`),
  UNIQUE KEY `kode_nilai` (`kode_nilai`),
  UNIQUE KEY `uniq_ujian_siswa` (`id_ujian`,`id_siswa`),
  KEY `id_siswa` (`id_siswa`),
  CONSTRAINT `trx_nilai_ibfk_1` FOREIGN KEY (`id_ujian`) REFERENCES `trx_jadwal` (`id_ujian`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `trx_nilai_ibfk_2` FOREIGN KEY (`id_siswa`) REFERENCES `mst_siswa` (`id_siswa`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trx_peserta`
--

DROP TABLE IF EXISTS `trx_peserta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trx_peserta` (
  `id_peserta` int(11) NOT NULL AUTO_INCREMENT,
  `kode_peserta` varchar(35) DEFAULT NULL,
  `id_ujian` int(11) DEFAULT NULL,
  `id_siswa` int(11) DEFAULT NULL,
  `no_peserta` varchar(20) DEFAULT NULL,
  `no_kursi` varchar(10) DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_peserta`),
  UNIQUE KEY `kode_peserta` (`kode_peserta`),
  UNIQUE KEY `uniq_ujian_kursi` (`id_ujian`,`no_kursi`),
  UNIQUE KEY `uniq_ujian_peserta` (`id_ujian`,`no_peserta`),
  KEY `id_siswa` (`id_siswa`),
  CONSTRAINT `trx_peserta_ibfk_1` FOREIGN KEY (`id_ujian`) REFERENCES `trx_jadwal` (`id_ujian`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `trx_peserta_ibfk_2` FOREIGN KEY (`id_siswa`) REFERENCES `mst_siswa` (`id_siswa`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `role` enum('admin','guru') NOT NULL,
  `id_guru` int(11) DEFAULT NULL,
  `photo_path` varchar(255) DEFAULT NULL,
  `photo_data` mediumblob DEFAULT NULL,
  `tanggal_buat` timestamp NOT NULL DEFAULT current_timestamp(),
  `tanggal_update` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `diubah_oleh` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `id_guru` (`id_guru`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`id_guru`) REFERENCES `mst_guru` (`id_guru`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `CONSTRAINT_1` CHECK (`role` = 'admin' and `id_guru` is null or `role` = 'guru' and `id_guru` is not null)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'penjadwalan_ujian_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-10 10:17:28
