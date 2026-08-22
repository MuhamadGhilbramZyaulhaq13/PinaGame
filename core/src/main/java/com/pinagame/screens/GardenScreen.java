package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.pinagame.core.DialogManager;

/**
 * Scene "Roblox-style" yang disederhanakan jadi 2D dari atas: avatar block/robot
 * Pina berjalan di map kebun ("Grow a Garden"), lalu memicu dialog begitu mendekati Datt.
 *
 * Semua visual (rumput, petak tanah, Moon Melon, karakter) di-generate lewat Pixmap
 * saat runtime, sama seperti PixelArtScreen -- gampang diganti ke sprite/gambar asli
 * nanti tanpa perlu ubah logika pergerakan/trigger sama sekali.
 *
 * Pergerakan & posisi NPC dihitung dalam "koordinat dunia" (WORLD_W x WORLD_H),
 * BUKAN piksel layar langsung -- supaya konsisten walau ukuran window berubah.
 * Konversi ke posisi layar dilakukan cuma pas menggambar (lihat render()).
 */
public class GardenScreen extends BaseGameScreen {

    private static final float WORLD_W = 400f;
    private static final float WORLD_H = 260f;
    private static final float SPEED = 90f; // unit dunia per detik

    private float pinaX = 60f, pinaY = 50f;
    private static final float DATT_X = 270f, DATT_Y = 128f;
    private final Rectangle datNpcTrigger = new Rectangle(240f, 108f, 60f, 50f);
    private boolean dialogTriggered = false;

    private SpriteBatch batch;
    private Texture gardenTexture;
    private Texture pinaTexture;
    private Texture dattTexture;

    public GardenScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        batch = new SpriteBatch();
        gardenTexture = buildGardenTexture();
        pinaTexture = buildCharacterTexture(new Color(0.55f, 0.75f, 0.85f, 1f), true);
        dattTexture = buildCharacterTexture(new Color(0.86f, 0.52f, 0.24f, 1f), false);
    }

    @Override
    public void render(float delta) {
        clearScreen();

        if (!dialogTriggered) {
            handleInput(delta);
            checkNpcTrigger();
        }

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Skala kanvas dunia (400x260) supaya pas di lebar layar, tetap jaga rasio,
        // rata atas -- sisa ruang bawah buat kotak dialog.
        float drawW = screenW;
        float drawH = screenW * (WORLD_H / WORLD_W);
        if (drawH > screenH) {
            drawH = screenH;
            drawW = screenH * (WORLD_W / WORLD_H);
        }
        float drawX = (screenW - drawW) / 2f;
        float drawY = screenH - drawH;
        float scale = drawW / WORLD_W;

        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        batch.begin();
        batch.draw(gardenTexture, drawX, drawY, drawW, drawH);
        drawCharacterSprite(dattTexture, drawX, drawY, scale, DATT_X, DATT_Y);
        drawCharacterSprite(pinaTexture, drawX, drawY, scale, pinaX, pinaY);
        batch.end();

        super.render(delta); // gambar overlay kotak dialog di atas scene
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
        if (gardenTexture != null) gardenTexture.dispose();
        if (pinaTexture != null) pinaTexture.dispose();
        if (dattTexture != null) dattTexture.dispose();
    }

    // ---------------------------------------------------------------------
    // Pergerakan & trigger (koordinat dunia)
    // ---------------------------------------------------------------------

    private void handleInput(float delta) {
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1;
        pinaX += dx * SPEED * delta;
        pinaY += dy * SPEED * delta;
        pinaX = MathUtils.clamp(pinaX, 10f, WORLD_W - 10f);
        pinaY = MathUtils.clamp(pinaY, 10f, WORLD_H - 10f);
    }

    private void checkNpcTrigger() {
        if (datNpcTrigger.contains(pinaX, pinaY)) {
            dialogTriggered = true;
            dialogManager.startFrom(null);
        }
    }

    // ---------------------------------------------------------------------
    // Gambar map & karakter
    // ---------------------------------------------------------------------

    private void drawCharacterSprite(Texture tex, float drawX, float drawY, float scale, float worldX, float worldY) {
        float spriteW = tex.getWidth() * scale;
        float spriteH = tex.getHeight() * scale;
        float screenX = drawX + worldX * scale - spriteW / 2f;
        float screenY = drawY + worldY * scale;
        batch.draw(tex, screenX, screenY, spriteW, spriteH);
    }

    private Texture buildGardenTexture() {
        int w = (int) WORLD_W, h = (int) WORLD_H;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        // Rumput dasar
        pm.setColor(new Color(0.36f, 0.56f, 0.29f, 1f));
        pm.fillRectangle(0, 0, w, h);

        // Tekstur bintik rumput
        pm.setColor(new Color(0.3f, 0.48f, 0.24f, 1f));
        for (int gx = 6; gx < w; gx += 22) {
            for (int gy = 6; gy < h; gy += 18) {
                pm.fillRectangle(gx, gy, 3, 3);
            }
        }

        // Petak tanah kosong (dekorasi)
        pm.setColor(new Color(0.42f, 0.3f, 0.19f, 1f));
        pm.fillRectangle(30, 190, 70, 45);
        pm.fillRectangle(300, 30, 60, 40);

        // Petak Moon Melon (tempat Datt berdiri)
        pm.fillRectangle(220, 140, 100, 70);
        pm.setColor(new Color(0.34f, 0.24f, 0.15f, 1f));
        pm.drawRectangle(220, 140, 100, 70);

        // Moon Melon raksasa
        pm.setColor(new Color(0.42f, 0.3f, 0.62f, 1f));
        pm.fillCircle(270, 175, 24);
        pm.setColor(new Color(0.58f, 0.46f, 0.78f, 1f));
        pm.fillCircle(263, 182, 8);

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    /** isRobot=true buat Pina (kepala abu-abu + "visor" biru), false buat manusia biasa. */
    private Texture buildCharacterTexture(Color shirtColor, boolean isRobot) {
        int w = 12, h = 18;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        pm.setColor(shirtColor);
        pm.fillRectangle(0, 0, w, 12);

        Color headColor = isRobot
            ? new Color(0.8f, 0.82f, 0.86f, 1f)
            : new Color(0.87f, 0.7f, 0.56f, 1f);
        pm.setColor(headColor);
        pm.fillRectangle(1, 12, w - 2, 6);

        if (isRobot) {
            pm.setColor(new Color(0.15f, 0.55f, 0.85f, 1f));
            pm.fillRectangle(2, 14, w - 4, 2);
        } else {
            pm.setColor(new Color(0.2f, 0.15f, 0.1f, 1f));
            pm.fillRectangle(1, 16, w - 2, 2);
        }

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }
}
