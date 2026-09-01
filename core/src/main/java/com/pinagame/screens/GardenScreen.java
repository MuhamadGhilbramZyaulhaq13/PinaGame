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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * Scene "Roblox-style" yang disederhanakan jadi 2D dari atas: avatar Pina berjalan
 * di map kebun ("Grow a Garden"), lalu memicu dialog begitu mendekati Datt.
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
 *
 * CATATAN: buildPinaTexture() versi ini hasil editan langsung oleh developer
 * (bukan versi awal saya) -- proporsi rambut/rok/sepatu disesuaikan lebih detail
 * berdasarkan referensi avatar aslinya.
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

    private float drawX, drawY, scale;

    private Label bubbleLabel;
    private Table bubbleBox;
    private String bubbleSpeaker;
    private Table dpad;
    private TextButton leftBtn, rightBtn, upBtn, downBtn;


    public GardenScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        batch = new SpriteBatch();
        gardenTexture = buildGardenTexture();
        pinaTexture = buildPinaTexture();
        dattTexture = buildDattTexture();
        buildDpad();

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

        if (!dialogTriggered) {
            handleInput(delta);
            checkNpcTrigger();
        }
        if (dpad != null) {
            dpad.setVisible(!dialogTriggered);
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
    // ---------------------------------------------------------------------

    private void handleInput(float delta) {
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || (leftBtn != null && leftBtn.isPressed())) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || (rightBtn != null && rightBtn.isPressed())) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || (upBtn != null && upBtn.isPressed())) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || (downBtn != null && downBtn.isPressed())) dy -= 1;
        pinaX += dx * SPEED * delta;
        pinaY += dy * SPEED * delta;
        pinaX = MathUtils.clamp(pinaX, 10f, WORLD_W - 10f);
        pinaY = MathUtils.clamp(pinaY, 10f, WORLD_H - 10f);
    }
    private void buildDpad() {
        leftBtn = new TextButton("<", skin);
        rightBtn = new TextButton(">", skin);
        upBtn = new TextButton("^", skin);
        downBtn = new TextButton("v", skin);

        Table cross = new Table();
        cross.add().size(64f);
        cross.add(upBtn).size(64f);
        cross.add().size(64f).row();
        cross.add(leftBtn).size(64f);
        cross.add().size(64f);
        cross.add(rightBtn).size(64f).row();
        cross.add().size(64f);
        cross.add(downBtn).size(64f);
        cross.add().size(64f);

        dpad = new Table();
        dpad.setFillParent(true);
        dpad.bottom().left().pad(24f);
        dpad.add(cross);

        uiStage.addActor(dpad);
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
        if (!dialogManager.isPausedAtWait()) {
            // Pina udah deket Datt, tapi dialog BELUM beneran nyampe titik jeda
            // (mis. pemain masih di tengah baca narasi pembuka sambil jalan) --
            // JANGAN lakukan apa-apa dulu. Method ini dicek ulang tiap frame
            // sampai dialog beneran paused.
            return;
        }
        dialogTriggered = true;
        dialogManager.continueFromPause();
    }

    // ---------------------------------------------------------------------
    // Bubble teks di atas kepala karakter
    // ---------------------------------------------------------------------

    @Override
    protected void showBubble(String speaker, String text) {
        ensureBubbleBuilt();
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
        bubbleBox.setTouchable(Touchable.disabled);
        bubbleBox.setVisible(false);
        uiStage.addActor(bubbleBox);
    }

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

    private Texture buildPinaTexture() {
        int w = 16, h = 24;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        Color skin = new Color(0.92f, 0.78f, 0.68f, 1f);
        Color hair = new Color(0.55f, 0.45f, 0.38f, 1f);
        Color blackeye = new Color(0.12f, 0.12f, 0.14f, 1f);
        Color whiteeye = new Color(0.92f, 0.92f, 0.92f, 1f);
        Color lips = new Color(0.94f, 0.59f, 0.67f, 1f);
        Color blouse = new Color(0.88f, 0.88f, 0.9f, 1f);
        Color blouseAccent = new Color(0.62f, 0.62f, 0.68f, 1f);
        Color blouseDark = new Color(0.25f, 0.25f, 0.35f, 1.0f);
        Color skirt = new Color(0.15f, 0.2f, 0.55f, 1f);
        Color shoes = new Color(0.92f, 0.92f, 0.92f, 1f);

        // Sepatu putih
        pm.setColor(shoes);
        pm.fillRectangle(3, 19, 10, 6);
        pm.setColor(skin);
        pm.fillRectangle(3, 19, 10, 4);

        // Rok biru
        pm.setColor(skirt);
        pm.fillRectangle(2, 17, 12, 3);
        pm.fillRectangle(12, 17, 2, 5);
        pm.fillRectangle(2, 19, 9, 2);
        pm.fillRectangle(2, 19, 6, 3);
        pm.fillRectangle(2, 19, 3, 4);

        // Atasan putih/abu dengan sedikit aksen motif
        pm.setColor(blouse);
        pm.fillRectangle(2, 11, 12, 6);

        pm.setColor(blouseAccent);
        pm.fillRectangle(4, 11, 2, 6);
        pm.fillRectangle(9, 11, 2, 6);

        pm.fillRectangle(2, 13, 12, 2);
        pm.setColor(blouseDark);
        pm.fillRectangle(4, 13, 2, 2);
        pm.fillRectangle(9, 13, 2, 2);

        // Wajah
        pm.setColor(skin);
        pm.fillRectangle(4, 5, 8, 5);
        pm.fillRectangle(5, 5, 6, 6);

        // Rambut atas
        pm.setColor(hair);
        pm.fillRectangle(3, 2, 10, 3);

        // Rambut panjang di kedua sisi, menutupi bahu
        pm.fillRectangle(2, 4, 1, 8);
        pm.fillRectangle(2, 4, 2, 6);
        pm.fillRectangle(2, 4, 3, 4);
        pm.fillRectangle(2, 4, 4, 2);
        pm.fillRectangle(11, 4, 4, 4);
        pm.fillRectangle(12, 4, 2, 7);

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
