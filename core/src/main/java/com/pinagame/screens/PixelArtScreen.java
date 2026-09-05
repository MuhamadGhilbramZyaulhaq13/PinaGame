package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pinagame.core.DialogManager;

/**
 * Scene dunia nyata bergaya pixel art retro (mis. Datt & Heri duduk di kamar
 * depan laptop). Digambar dengan cara "lukis di kanvas kecil, lalu diperbesar
 * TANPA smoothing (Nearest filter)" -- teknik umum buat efek blocky/pixelated.
 *
 * CANVAS_W:CANVAS_H sengaja 200:90 (~2.22:1) -- mendekati rasio layar HP
 * landscape modern -- supaya bilah hitam kiri-kanan minimal di HP sungguhan.
 */
public class PixelArtScreen extends BaseGameScreen {

    private static final int CANVAS_W = 200;
    private static final int CANVAS_H = 90;
    private static final float BUBBLE_Y_OFFSET = 6f;

    private static final float DATT_HEAD_X = 79f, DATT_HEAD_TOP_Y = 44f;
    private static final float HERI_HEAD_X = 139f, HERI_HEAD_TOP_Y = 44f;

    private Texture roomTexture;
    private SpriteBatch batch;

    private Label bubbleLabel;
    private Table bubbleBox;
    private String bubbleSpeaker;

    public PixelArtScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        batch = new SpriteBatch();
        roomTexture = buildRoomTexture();
    }

    @Override
    public void render(float delta) {
        clearScreen();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float drawW = screenW;
        float drawH = screenW * ((float) CANVAS_H / CANVAS_W);
        if (drawH > screenH) {
            drawH = screenH;
            drawW = screenH * ((float) CANVAS_W / CANVAS_H);
        }
        float drawX = (screenW - drawW) / 2f;
        float drawY = screenH - drawH;
        float scale = drawW / CANVAS_W;

        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        batch.begin();
        batch.draw(roomTexture, drawX, drawY, drawW, drawH);
        batch.end();

        updateBubblePosition(drawX, drawY, scale);

        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (roomTexture != null) roomTexture.dispose();
        if (batch != null) batch.dispose();
    }

    // ---------------------------------------------------------------------
    // Bubble teks Datt & Heri
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

    private void updateBubblePosition(float drawX, float drawY, float scale) {
        if (bubbleBox == null || !bubbleBox.isVisible()) return;
        boolean isHeri = "Heri".equals(bubbleSpeaker);
        float headX = isHeri ? HERI_HEAD_X : DATT_HEAD_X;
        float headTopY = isHeri ? HERI_HEAD_TOP_Y : DATT_HEAD_TOP_Y;

        float bx = drawX + headX * scale;
        float by = drawY + (CANVAS_H - (headTopY - BUBBLE_Y_OFFSET)) * scale;
        bubbleBox.setPosition(bx - bubbleBox.getWidth() / 2f, by);
    }

    // ---------------------------------------------------------------------
    // Gambar adegan kamar ke kanvas kecil
    // ---------------------------------------------------------------------

    private Texture buildRoomTexture() {
        Pixmap pm = new Pixmap(CANVAS_W, CANVAS_H, Pixmap.Format.RGBA8888);

        pm.setColor(new Color(0.55f, 0.47f, 0.4f, 1f));
        pm.fillRectangle(0, 0, CANVAS_W, 55);
        pm.setColor(new Color(0.36f, 0.25f, 0.2f, 1f));
        pm.fillRectangle(0, 55, CANVAS_W, CANVAS_H - 55);

        pm.setColor(new Color(0.55f, 0.75f, 0.92f, 1f));
        pm.fillRectangle(22, 8, 35, 22);
        pm.setColor(new Color(0.9f, 0.87f, 0.8f, 1f));
        pm.drawRectangle(22, 8, 35, 22);
        pm.drawLine(39, 8, 39, 30);
        pm.drawLine(22, 19, 57, 19);

        pm.setColor(new Color(0.32f, 0.21f, 0.13f, 1f));
        pm.fillRectangle(69, 58, 75, 6);
        pm.fillRectangle(72, 64, 4, 20);
        pm.fillRectangle(136, 64, 4, 20);

        pm.setColor(new Color(0.18f, 0.18f, 0.2f, 1f));
        pm.fillRectangle(94, 54, 25, 4);
        pm.fillRectangle(96, 40, 20, 14);
        pm.setColor(new Color(0.45f, 0.75f, 0.95f, 1f));
        pm.fillRectangle(99, 42, 15, 10);

        drawDattCharacter(pm, 72);
        drawSimpleCharacter(pm, 132, new Color(0.32f, 0.62f, 0.52f, 1f));

        Texture tex = new Texture(pm);
        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose();
        return tex;
    }

    private void drawSimpleCharacter(Pixmap pm, int baseX, Color shirtColor) {
        pm.setColor(shirtColor);
        pm.fillRectangle(baseX, 58, 14, 20);
        pm.setColor(new Color(0.87f, 0.7f, 0.56f, 1f));
        pm.fillRectangle(baseX + 3, 48, 8, 10);
        pm.setColor(new Color(0.2f, 0.15f, 0.1f, 1f));
        pm.fillRectangle(baseX + 2, 46, 10, 4);
    }

    private void drawDattCharacter(Pixmap pm, int baseX) {
        Color skin = new Color(0.87f, 0.72f, 0.58f, 1f);
        Color hair = new Color(0.35f, 0.22f, 0.13f, 1f);
        Color jacket = new Color(0.12f, 0.12f, 0.14f, 1f);
        Color shirt = new Color(0.15f, 0.5f, 0.85f, 1f);

        pm.setColor(jacket);
        pm.fillRectangle(baseX, 58, 14, 20);
        pm.setColor(shirt);
        pm.fillRectangle(baseX + 5, 58, 4, 20);

        pm.setColor(skin);
        pm.fillRectangle(baseX + 3, 48, 8, 10);

        pm.setColor(hair);
        pm.fillRectangle(baseX + 2, 46, 10, 4);
        pm.fillRectangle(baseX + 8, 49, 3, 3);
    }
}
