package co.id.dao;

/**
 * Data yang mau disimpan sudah diubah orang lain sejak dibuka (mis. dua admin membuka sesi Penempatan Ruangan
 * yang sama). Penyimpanan dibatalkan supaya perubahan admin lain tidak tertimpa tanpa disadari.
 */
public class ConcurrentChangeException extends IllegalStateException {
    public ConcurrentChangeException(String message) {
        super(message);
    }
}