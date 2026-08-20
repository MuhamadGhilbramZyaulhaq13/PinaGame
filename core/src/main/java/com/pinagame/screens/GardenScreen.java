package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.pinagame.core.DialogManager;

/**
 * Scene "Roblox-style": avatar block/robot Pina berjalan di map kebun ("Grow a Garden").
 *
 * Untuk render 3D low-poly sesungguhnya, ganti bagian TODO di render() dengan
 * ModelBatch + Environment dari libGDX 3D API (com.badlogic.gdx.graphics.g3d.*).
 * File ini fokus ke LOGIKA pergerakan & trigger dialog, bukan detail render 3D,
 * supaya arsitekturnya jelas terlepas dari pilihan teknik render akhir.
 */
public class GardenScreen extends BaseGameScreen {

    private float pinaX = 100, pinaY = 100;
    private static final float SPEED = 150f;

    // Posisi & ukuran contoh area trigger NPC Datt — sesuaikan dengan layout map asli.
    private final Rectangle datNpcTrigger = new Rectangle(300, 300, 64, 64);
    private boolean dialogTriggered = false;

    public GardenScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
    }

    @Override
    public void render(float delta) {
        if (!dialogTriggered) {
            handleInput(delta);
            checkNpcTrigger();
        }

        // TODO: gambar map "Grow a Garden" + model block Pina & Datt di sini
        // (SpriteBatch untuk versi 2.5D top-down, atau ModelBatch untuk versi 3D penuh)

        super.render(delta); // gambar overlay kotak dialog di atas scene
    }

    private void handleInput(float delta) {
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1;
        pinaX += dx * SPEED * delta;
        pinaY += dy * SPEED * delta;
    }

    private void checkNpcTrigger() {
        if (datNpcTrigger.contains(pinaX, pinaY)) {
            dialogTriggered = true;
            // Mulai dari startNode chapter yang sedang aktif (di-load oleh ChapterManager).
            dialogManager.startFrom(null);
        }
    }
}
