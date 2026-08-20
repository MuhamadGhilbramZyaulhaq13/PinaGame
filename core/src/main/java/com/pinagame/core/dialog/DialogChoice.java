package com.pinagame.core.dialog;

/**
 * Satu opsi pilihan dalam sebuah node dialog.
 * Contoh di JSON:
 * { "text": "Wih beneran? Makasih banyak!!", "next": "d1_008", "setFlag": "d1_pina_grateful" }
 */
public class DialogChoice {

    /** Teks yang ditampilkan sebagai tombol pilihan. */
    public String text;

    /** ID node tujuan setelah pilihan ini diambil. */
    public String next;

    /** Opsional: nama flag yang di-set ketika pilihan ini dipilih. */
    public String setFlag;

    /** Opsional: nilai flag (default "true" kalau tidak diisi). */
    public String setFlagValue;
}
