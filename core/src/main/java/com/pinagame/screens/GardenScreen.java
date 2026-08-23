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
 * Posisi awal Pina & Datt beda tergantung ini kunjungan PERTAMA (Hari 1, nunggu
 * trigger jalan-mendekat) atau kunjungan SUSULAN (mis. Hari 4 balik dari Instagram,
 * langsung mulai dengan Datt "jalan nyamperin" Pina dari kejauhan).
 */
public class GardenScreen extends BaseGameScreen {

    private static final float WORLD_W = 400f;
    private static final float WORLD_H = 260f;
    private static final float SPEED = 90f; // kecepatan Pina, unit dunia/detik
    private static final float DATT_WALK_SPEED = 150f; // Datt jalan lebih cepat, kesan "buru-buru"

    private float pinaX, pinaY;
    private float dattX, dattY;
    private float dattTargetX, dattTargetY;
    private boolean dattWalking = false;

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

        if (dialogManager.hasStarted()) {
            // Kunjungan SUSULAN (bukan Hari 1) -- dialog udah jalan dari tempat lain.
            // Posisi beda dari Hari 1, dan Datt "jalan nyamperin" Pina dari kejauhan.
            pinaX = 180f;
            pinaY = 55f;
            dattX = 370f;
            dattY = 220f;
            dattTargetX = 215f;
            dattTargetY = 95f;
            dattWalking = true;
            dialogTriggered = true; // gak perlu nunggu trigger lagi, dialog udah aktif
        } else {
            // Kunjungan PERTAMA (Hari 1) -- posisi & perilaku seperti semula.
            pinaX = 60f;
            pinaY = 50f;
            dattX = 270f;
            dattY = 128f;
            dattWalking = false;
        }
    }

    @Override
    public void render(float delta) {
        clearScreen();

        // Pergerakan Pina tetap aktif kapan pun (biar tetap "hidup" walau dialog
        // lagi jalan sendiri), tapi TRIGGER buat mulai dialog cuma dicek kalau
        // memang belum pernah trigger di screen ini.
        handleInput(delta);
        if (!dialogTriggered) {
            checkNpcTrigger();
        }
        updateDattWalk(delta);

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
        drawCharacterSprite(dattTexture, drawX, drawY, scale, dattX, dattY);
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
    // Pergerakan, jalan-nyamperin Datt, & trigger (koordinat dunia)
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

    /** Gerakin Datt pelan-pelan menuju dattTargetX/Y kalau lagi dalam mode "jalan nyamperin". */
    private void updateDattWalk(float delta) {
        if (!dattWalking) return;
        float dx = dattTargetX - dattX;
        float dy = dattTargetY - dattY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 4f) {
            dattX = dattTargetX;
            dattY = dattTargetY;
            dattWalking = false;
            return;
        }
        float step = DATT_WALK_SPEED * delta;
        dattX += (dx / dist) * step;
        dattY += (dy / dist) * step;
    }

    private void checkNpcTrigger() {
        if (dialogManager.hasStarted()) {
            // Dialog udah jalan dari tempat lain -- JANGAN restart ke awal cerita.
            dialogTriggered = true;
            return;
        }
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

        // Petak Moon Melon
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
