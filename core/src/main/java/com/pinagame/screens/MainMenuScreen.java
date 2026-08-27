package com.pinagame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.pinagame.GameMain;
import com.pinagame.core.SaveData;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Layar pertama yang tampil saat game dibuka -- sebelum masuk ke chapter manapun.
 * Menampilkan tombol "Lanjutkan" (kalau ada save) dan "Mulai Baru".
 *
 * Background di sini sengaja pakai gradasi halus (bukan gaya pixel-art blocky
 * seperti screen gameplay) supaya menu terasa "tenang", beda nuansa dari gameplay.
 */
public class MainMenuScreen extends ScreenAdapter {

    private final GameMain game;
    private final boolean hasSave;
    private final SaveData saveData;

    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;
    private Texture backgroundTexture;

    public MainMenuScreen(GameMain game, boolean hasSave, SaveData saveData) {
        this.game = game;
        this.hasSave = hasSave;
        this.saveData = saveData;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        batch = new SpriteBatch();
        backgroundTexture = buildBackgroundTexture();

        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label("PINA", skin);
        title.setFontScale(3f);
        title.setColor(Color.WHITE);
        root.add(title).padBottom(8).row();

        Label subtitle = new Label("sebuah kisah dari 'Grow a Garden'", skin);
        subtitle.setColor(new Color(0.82f, 0.82f, 0.82f, 1f));
        root.add(subtitle).padBottom(48).row();

        if (hasSave) {
            Label saveInfo = new Label(buildSaveInfoText(), skin);
            saveInfo.setColor(new Color(0.75f, 0.9f, 0.78f, 1f));
            root.add(saveInfo).padBottom(18).row();

            TextButton continueBtn = new TextButton("Lanjutkan", skin);
            continueBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.continueGame();
                }
            });
            root.add(continueBtn).width(240).height(52).padBottom(16).row();
        }

        TextButton newGameBtn = new TextButton(hasSave ? "Mulai Baru (hapus progres)" : "Mulai", skin);
        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.startNewGame();
            }
        });
        root.add(newGameBtn).width(240).height(52).row();

        stage.addActor(root);
    }

    private String buildSaveInfoText() {
        String info = "Progres tersimpan -- Chapter " + saveData.currentChapter;
        if (saveData.lastSavedAtMillis > 0) {
            String time = new SimpleDateFormat("dd MMM yyyy, HH:mm").format(new Date(saveData.lastSavedAtMillis));
            info += "\nTerakhir disimpan: " + time;
        }
        return info;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH));
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, screenW, screenH);
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        backgroundTexture.dispose();
    }

    /** Gradasi hijau gelap sederhana -- nuansa kebun di malam hari. */
    private Texture buildBackgroundTexture() {
        int w = 2, h = 64;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            float t = y / (float) h;
            pm.setColor(new Color(0.06f + t * 0.08f, 0.14f + t * 0.16f, 0.09f + t * 0.09f, 1f));
            pm.drawLine(0, y, w, y);
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }
}
