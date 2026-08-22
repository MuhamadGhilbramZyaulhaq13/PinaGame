package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pinagame.core.DialogManager;

/**
 * Scene dunia nyata bergaya pixel art retro (mis. Datt & Heri duduk di kamar
 * depan laptop). Digambar dengan cara "lukis di kanvas kecil (160x90px), lalu
 * diperbesar TANPA smoothing (Nearest filter)" -- teknik umum buat dapet efek
 * blocky/pixelated tanpa perlu file gambar asli.
 *
 * Gampang diganti ke sprite/gambar sungguhan nanti: cukup ganti isi
 * buildRoomTexture() jadi load Texture dari assets, bukan gambar Pixmap manual --
 * struktur render() (skala & posisi ke layar) tidak perlu diubah sama sekali.
 */
public class PixelArtScreen extends BaseGameScreen {

    private static final int CANVAS_W = 160;
    private static final int CANVAS_H = 90;

    private Texture roomTexture;
    private SpriteBatch batch;

    public PixelArtScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        batch = new SpriteBatch();
        roomTexture = buildRoomTexture();
        // Sama seperti sebelumnya: screen ini selalu dimasuki lewat CHANGE_SCENE
        // di tengah dialog yang sudah berjalan, jadi tidak perlu startFrom() manual.
    }

    @Override
    public void render(float delta) {
        clearScreen();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Skala kanvas kecil supaya pas memenuhi lebar layar, tetap jaga rasio
        // aslinya (160:90), rata atas -- sisa ruang bawah buat kotak dialog.
        float drawW = screenW;
        float drawH = screenW * ((float) CANVAS_H / CANVAS_W);
        if (drawH > screenH) {
            drawH = screenH;
            drawW = screenH * ((float) CANVAS_W / CANVAS_H);
        }
        float drawX = (screenW - drawW) / 2f;
        float drawY = screenH - drawH;

        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        batch.begin();
        batch.draw(roomTexture, drawX, drawY, drawW, drawH);
        batch.end();

        super.render(delta); // gambar overlay kotak dialog di atas scene
    }

    @Override
    public void dispose() {
        super.dispose();
        if (roomTexture != null) roomTexture.dispose();
        if (batch != null) batch.dispose();
    }

    // ---------------------------------------------------------------------
    // Gambar adegan kamar ke kanvas kecil
    // ---------------------------------------------------------------------

    private Texture buildRoomTexture() {
        Pixmap pm = new Pixmap(CANVAS_W, CANVAS_H, Pixmap.Format.RGBA8888);

        // Dinding
        pm.setColor(new Color(0.55f, 0.47f, 0.4f, 1f));
        pm.fillRectangle(0, 0, CANVAS_W, 55);
        // Lantai
        pm.setColor(new Color(0.36f, 0.25f, 0.2f, 1f));
        pm.fillRectangle(0, 55, CANVAS_W, CANVAS_H - 55);

        // Jendela
        pm.setColor(new Color(0.55f, 0.75f, 0.92f, 1f));
        pm.fillRectangle(18, 8, 28, 22);
        pm.setColor(new Color(0.9f, 0.87f, 0.8f, 1f));
        pm.drawRectangle(18, 8, 28, 22);
        pm.drawLine(32, 8, 32, 30);
        pm.drawLine(18, 19, 46, 19);

        // Meja
        pm.setColor(new Color(0.32f, 0.21f, 0.13f, 1f));
        pm.fillRectangle(55, 58, 60, 6);
        pm.fillRectangle(58, 64, 4, 20);
        pm.fillRectangle(108, 64, 4, 20);

        // Laptop (layar menyala biru muda)
        pm.setColor(new Color(0.18f, 0.18f, 0.2f, 1f));
        pm.fillRectangle(76, 54, 20, 4);
        pm.fillRectangle(78, 40, 16, 14);
        pm.setColor(new Color(0.45f, 0.75f, 0.95f, 1f));
        pm.fillRectangle(80, 42, 12, 10);

        // Karakter Datt (kiri) & Heri (kanan), duduk di depan meja
        drawSimpleCharacter(pm, 58, new Color(0.86f, 0.52f, 0.24f, 1f));
        drawSimpleCharacter(pm, 100, new Color(0.32f, 0.62f, 0.52f, 1f));

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    private void drawSimpleCharacter(Pixmap pm, int baseX, Color shirtColor) {
        pm.setColor(shirtColor);
        pm.fillRectangle(baseX, 58, 14, 20); // badan
        pm.setColor(new Color(0.87f, 0.7f, 0.56f, 1f));
        pm.fillRectangle(baseX + 3, 48, 8, 10); // kepala
        pm.setColor(new Color(0.2f, 0.15f, 0.1f, 1f));
        pm.fillRectangle(baseX + 2, 46, 10, 4); // rambut
    }
}
