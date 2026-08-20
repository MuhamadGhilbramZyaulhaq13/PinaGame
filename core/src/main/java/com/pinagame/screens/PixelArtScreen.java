package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.pinagame.core.DialogManager;

/**
 * Scene dunia nyata bergaya pixel art retro (mis. Datt & Heri duduk di kamar
 * depan laptop). Sifatnya lebih statis (adegan percakapan, bukan eksplorasi bebas),
 * sehingga screen ini cukup menampilkan background + sprite karakter idle,
 * dan sepenuhnya digerakkan oleh DialogManager (auto-lanjut per baris).
 */
public class PixelArtScreen extends BaseGameScreen {

    public PixelArtScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        // TIDAK memanggil startFrom()/advance() di sini: pada Chapter 1, screen ini selalu
        // dimasuki lewat action "CHANGE_SCENE" dari tengah-tengah dialog yang sudah berjalan
        // (lihat DialogManager#goToNode) — jadi baris berikutnya otomatis terkirim ke listener
        // baru (screen ini) begitu terdaftar. Kalau suatu saat chapter baru butuh scene ini
        // sebagai TITIK AWAL chapter (bukan hasil transisi), panggil dialogManager.startFrom(null) di sini.
    }

    @Override
    public void render(float delta) {
        // TODO: gambar background pixel-art sesuai sceneId (mis. "PIXEL_ROOM_DATT_HERI")
        // dan sprite Datt/Heri (idle animation via TextureAtlas + Animation<TextureRegion>).
        // Palet retro: gunakan Texture dengan setFilter(Nearest, Nearest) supaya tidak blur.

        super.render(delta);
    }
}
