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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pinagame.core.DialogManager;

/**
 * Scene "Roblox-style" yang disederhanakan jadi 2D dari atas: avatar block/robot
 * Pina berjalan di map kebun ("Grow a Garden"), lalu memicu dialog begitu mendekati Datt.
 *
 * Ada 2 "mode" tergantung bagaimana screen ini dimasuki:
 * - WALK_TO_DATT: dialog (narasi pembuka) baru saja mulai/lanjut otomatis, lalu
 *   PAUSE di node WAIT_APPROACH menunggu pemain jalanin Pina mendekati Datt.
 *   Dipakai di Hari 1 (pertemuan pertama) DAN tiap "jeda hari" (Hari 2, Hari 3).
 * - AUTO_CONTINUE: kunjungan di tengah cerita yang sudah aktif (mis. Hari 4 balik
 *   dari Instagram) -- Datt yang jalan menghampiri Pina, tidak perlu trigger.
 *
 * Begitu dialog ke-trigger (dialogTriggered=true), gerakan Pina DIKUNCI -- supaya
 * pemain fokus tap layar buat baca obrolan, bukan keliaran jalan-jalan.
 */
public class GardenScreen extends BaseGameScreen {

    private enum EntryMode { WALK_TO_DATT, AUTO_CONTINUE }

    private static final float WORLD_W = 400f;
    private static final float WORLD_H = 260f;
    private static final float SPEED = 90f;
    private static final float DATT_WALK_SPEED = 150f;
    private static final float FADE_DURATION = 0.7f;
    private static final float BUBBLE_GAP = 8f; // jarak bersih antara puncak kepala dan bubble, dalam piksel layar

    private EntryMode entryMode;

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

    // Posisi layar hasil transform dunia->layar, dihitung ulang tiap frame di render()
    // dan dipakai bareng buat gambar karakter DAN posisi bubble.
    private float drawX, drawY, scale;

    private Label bubbleLabel;
    private Table bubbleBox;
    private String bubbleSpeaker;

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
        dattTexture = buildDattTexture();

        entryMode = resolveEntryMode();

        switch (entryMode) {
            case AUTO_CONTINUE:
                pinaX = 180f;
                pinaY = 55f;
                dattX = 370f;
                dattY = 220f;
                dattTargetX = 215f;
                dattTargetY = 95f;
                dattWalking = true;
                dialogTriggered = true;
                break;
            case WALK_TO_DATT:
            default:
                pinaX = 60f;
                pinaY = 50f;
                dattX = 270f;
                dattY = 128f;
                dattWalking = false;
                dialogTriggered = false;
                addFadeInOverlay(FADE_DURATION);
                break;
        }
    }

    private EntryMode resolveEntryMode() {
        if (!dialogManager.hasStarted()) {
            return EntryMode.WALK_TO_DATT;
        }
        if (sceneId != null && sceneId.startsWith("GARDEN_DAY")) {
            return EntryMode.WALK_TO_DATT;
        }
        return EntryMode.AUTO_CONTINUE;
    }

    @Override
    public void render(float delta) {
        clearScreen();

        // Gerakan (+ cek trigger) HANYA aktif sebelum dialog ke-trigger. Begitu
        // dialogTriggered true, Pina "diam di tempat" -- fokus pemain pindah ke tap
        // layar buat lanjutin obrolan, bukan gerak-gerakin karakter lagi.
        if (!dialogTriggered) {
            handleInput(delta);
            checkNpcTrigger();
        }
        updateDattWalk(delta);

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float drawW = screenW;
        float drawH = screenW * (WORLD_H / WORLD_W);
        if (drawH > screenH) {
            drawH = screenH;
            drawW = screenH * (WORLD_W / WORLD_H);
        }
        drawX = (screenW - drawW) / 2f;
        drawY = screenH - drawH;
        scale = drawW / WORLD_W;

        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        batch.begin();
        batch.draw(gardenTexture, drawX, drawY, drawW, drawH);
        drawCharacterSprite(dattTexture, drawX, drawY, scale, dattX, dattY);
        drawCharacterSprite(pinaTexture, drawX, drawY, scale, pinaX, pinaY);
        batch.end();

        updateBubblePosition();

        super.render(delta);
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
        if (entryMode == EntryMode.AUTO_CONTINUE) {
            dialogTriggered = true;
            return;
        }
        if (!datNpcTrigger.contains(pinaX, pinaY)) {
            return;
        }
        dialogTriggered = true;
        // Dialog sudah otomatis jalan sampai node WAIT_APPROACH (narasi pembuka
        // sudah tampil) -- ini lanjut dari titik jeda situ ke obrolan karakter.
        dialogManager.continueFromPause();
    }

    // ---------------------------------------------------------------------
    // Bubble teks di atas kepala karakter
    // ---------------------------------------------------------------------

    @Override
    protected void showBubble(String speaker, String text) {
        ensureBubbleBuilt();
        // Lebar menyesuaikan layar (maks 230px atau separuh lebar layar, mana yang
        // lebih kecil) -- supaya teks melebar ke samping, bukan numpuk jadi banyak
        // baris ke bawah yang bisa nutupin karakternya.
        float maxWidth = Math.min(230f, Gdx.graphics.getWidth() * 0.5f);
        bubbleLabel.setText(text);
        bubbleBox.clear();
        bubbleBox.add(bubbleLabel).width(maxWidth).pad(8);
        bubbleBox.pack();
        bubbleBox.setVisible(true);
        bubbleSpeaker = speaker;
    }

    @Override
    protected void hideBubble() {
        if (bubbleBox != null) bubbleBox.setVisible(false);
        bubbleSpeaker = null;
    }

    private void ensureBubbleBuilt() {
        if (bubbleBox != null) return;
        bubbleLabel = new Label("", skin);
        bubbleLabel.setWrap(true);
        bubbleLabel.setColor(Color.BLACK);
        bubbleLabel.setFontScale(0.9f);
        bubbleLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        bubbleBox = new Table();
        bubbleBox.setBackground(skin.newDrawable("white", new Color(1f, 1f, 1f, 0.94f)));
        bubbleBox.setTouchable(Touchable.disabled); // jangan sampai nge-block klik "lanjut"
        bubbleBox.setVisible(false);
        uiStage.addActor(bubbleBox);
    }

    /**
     * Dipanggil tiap frame di render() supaya bubble ikut posisi karakter yang lagi
     * jalan. Posisi Y dihitung dari TINGGI ASLI sprite yang lagi ngomong (bukan
     * angka tetap), supaya bubble selalu bersih di atas kepala berapa pun ukuran
     * layarnya -- sebelumnya pakai offset tetap yang bisa kepotong/nutupin
     * karakter di layar kecil.
     */
    private void updateBubblePosition() {
        if (bubbleBox == null || !bubbleBox.isVisible()) return;
        boolean isDatt = "Datt".equals(bubbleSpeaker);
        float worldX = isDatt ? dattX : pinaX;
        float worldY = isDatt ? dattY : pinaY;
        Texture speakerTex = isDatt ? dattTexture : pinaTexture;

        float charTopScreenY = drawY + worldY * scale + speakerTex.getHeight() * scale;
        float bx = drawX + worldX * scale;
        float by = charTopScreenY + BUBBLE_GAP;
        bubbleBox.setPosition(bx - bubbleBox.getWidth() / 2f, by);
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

        pm.setColor(new Color(0.36f, 0.56f, 0.29f, 1f));
        pm.fillRectangle(0, 0, w, h);

        pm.setColor(new Color(0.3f, 0.48f, 0.24f, 1f));
        for (int gx = 6; gx < w; gx += 22) {
            for (int gy = 6; gy < h; gy += 18) {
                pm.fillRectangle(gx, gy, 3, 3);
            }
        }

        pm.setColor(new Color(0.42f, 0.3f, 0.19f, 1f));
        pm.fillRectangle(30, 190, 70, 45);
        pm.fillRectangle(300, 30, 60, 40);

        pm.fillRectangle(220, 140, 100, 70);
        pm.setColor(new Color(0.34f, 0.24f, 0.15f, 1f));
        pm.drawRectangle(220, 140, 100, 70);

        pm.setColor(new Color(0.42f, 0.3f, 0.62f, 1f));
        pm.fillCircle(270, 175, 24);
        pm.setColor(new Color(0.58f, 0.46f, 0.78f, 1f));
        pm.fillCircle(263, 182, 8);

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    private Texture buildCharacterTexture(Color shirtColor, boolean isRobot) {
        int w = 12, h = 18;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        Color headColor = isRobot
            ? new Color(0.8f, 0.82f, 0.86f, 1f)
            : new Color(0.87f, 0.7f, 0.56f, 1f);
        pm.setColor(headColor);
        pm.fillRectangle(1, 0, w - 2, 6);

        if (isRobot) {
            pm.setColor(new Color(0.15f, 0.55f, 0.85f, 1f));
            pm.fillRectangle(2, 2, w - 4, 2);
        } else {
            pm.setColor(new Color(0.2f, 0.15f, 0.1f, 1f));
            pm.fillRectangle(1, 0, w - 2, 2);
        }

        pm.setColor(shirtColor);
        pm.fillRectangle(0, 6, w, 12);

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    private Texture buildDattTexture() {
        int w = 16, h = 24;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        Color skin = new Color(0.87f, 0.72f, 0.58f, 1f);
        Color hair = new Color(0.35f, 0.22f, 0.13f, 1f);
        Color jacket = new Color(0.12f, 0.12f, 0.14f, 1f);
        Color shirt = new Color(0.15f, 0.5f, 0.85f, 1f);
        Color pants = new Color(0.32f, 0.36f, 0.24f, 1f);

        pm.setColor(skin);
        pm.fillRectangle(2, 5, 12, 6);

        pm.setColor(hair);
        pm.fillRectangle(1, 0, 14, 5);
        pm.fillRectangle(10, 4, 4, 4);

        pm.setColor(jacket);
        pm.fillRectangle(0, 11, 16, 9);
        pm.setColor(shirt);
        pm.fillRectangle(6, 11, 4, 9);

        pm.setColor(pants);
        pm.fillRectangle(1, 20, 14, 4);

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }
}
