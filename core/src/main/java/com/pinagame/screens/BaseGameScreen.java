package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.pinagame.core.DialogManager;
import com.pinagame.core.dialog.DialogChoice;

import java.util.List;

/**
 * Induk untuk GardenScreen / PixelArtScreen / SocialMediaScreen.
 * Menyediakan overlay kotak dialog + tombol pilihan yang seragam, supaya UI
 * dialog konsisten walau visual di belakangnya beda gaya (3D / pixel art / UI HP).
 *
 * Subclass WAJIB memanggil buildDialogueUI() di show(), dan bertanggung jawab
 * menggambar konten visualnya sendiri sebelum memanggil super.render(delta).
 */
public abstract class BaseGameScreen extends ScreenAdapter implements DialogManager.DialogListener {

    protected final Game game;
    protected final DialogManager dialogManager;
    protected final String sceneId;

    protected Stage uiStage;
    protected Skin skin; // TODO: load dari assets/ui/skin.json (skin bawaan libGDX "uiskin" cukup untuk prototipe)
    protected Table dialogueBox;
    protected Label speakerLabel;
    protected Label lineLabel;
    protected Table choiceContainer;

    public BaseGameScreen(Game game, DialogManager dialogManager, String sceneId) {
        this.game = game;
        this.dialogManager = dialogManager;
        this.sceneId = sceneId;
    }

    /**
     * WAJIB dipanggil subclass sebagai baris PALING PERTAMA di render(), sebelum
     * menggambar apa pun. Tanpa ini, frame lama tidak terhapus dan hasilnya
     * "ghosting"/dobel — konten frame sebelumnya numpuk sama frame baru.
     */
    protected void clearScreen() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
    }

    protected void buildDialogueUI() {
        uiStage = new Stage(new ScreenViewport());
        skin = loadSkin();

        speakerLabel = new Label("", skin);
        lineLabel = new Label("", skin);
        lineLabel.setWrap(true);
        choiceContainer = new Table();

        // PENTING: Label itu Actor sungguhan (beda dari Table/Group), jadi kalau
        // diklik dia "menangkap" klik itu sendiri dan TIDAK diteruskan ke clickCatcher
        // di belakangnya -- padahal Label ini sendiri belum punya listener apa pun.
        // Efeknya: klik yang kena PERSIS di atas tulisan dialog terasa tidak ngefek
        // sama sekali. Makanya listener "lanjut" yang sama juga ditempel di sini.
        speakerLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialogManager.advance();
            }
        });
        lineLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialogManager.advance();
            }
        });

        Table box = new Table();
        box.add(speakerLabel).left().row();
        box.add(lineLabel).width(600).left().padTop(4).row();
        box.add(choiceContainer).padTop(8).row();

        dialogueBox = new Table();
        dialogueBox.setFillParent(true);
        dialogueBox.bottom().padBottom(24);
        dialogueBox.add(box);

        // PENTING: "klik di mana saja untuk lanjut" TIDAK bisa ditempel langsung
        // ke uiStage/root, karena Stage cuma memicu listener kalau klik itu
        // benar-benar mengenai sebuah Actor. Area kosong (bukan di atas teks/tombol)
        // dianggap "tidak kena apa-apa", jadi listener di root tidak pernah terpanggil.
        // Solusinya: pasang Actor tak terlihat sebesar mungkin sebagai "penangkap klik",
        // ditambahkan LEBIH DULU (jadi lapisan paling belakang) supaya tombol pilihan
        // tetap bisa diklik secara terpisah di depannya.
        Actor clickCatcher = new Actor();
        clickCatcher.setBounds(-10000, -10000, 20000, 20000);
        clickCatcher.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialogManager.advance();
            }
        });

        uiStage.addActor(clickCatcher); // ditambah duluan -> lapisan paling belakang
        uiStage.addActor(dialogueBox);  // ditambah belakangan -> lapisan paling depan

        // WAJIB: tanpa ini, Stage tidak pernah menerima event klik/tap sama sekali
        // dari libGDX, jadi clickCatcher maupun tombol pilihan tidak akan pernah bereaksi.
        Gdx.input.setInputProcessor(uiStage);
    }

    /** Ganti implementasi ini untuk load skin.json asli dari assets/ui/. */
    protected Skin loadSkin() {
        // Placeholder: di project nyata, load com.badlogic.gdx.files.FileHandle skin.json
        // return new Skin(Gdx.files.internal("ui/skin.json"));
        throw new UnsupportedOperationException(
            "Sediakan Skin (mis. uiskin default libGDX) sebelum build UI dialog.");
    }

    /**
     * Overlay hitam penuh layar yang otomatis fade-out begitu screen ini tampil --
     * efek "napas" transisi. Panggil ini di show() SETELAH buildDialogueUI(), supaya
     * ditambahkan paling atas (lapisan terdepan) dan menutupi scene sesaat sebelum
     * memudar. Tidak menghalangi klik sama sekali (Touchable.disabled) -- durasi
     * transisi cuma visual, tidak pernah bikin pemain "kehilangan" klik.
     */
    protected void addFadeInOverlay(float durationSeconds) {
        Image overlay = new Image(skin.getDrawable("white"));
        overlay.setColor(Color.BLACK);
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.disabled);
        overlay.addAction(Actions.fadeOut(durationSeconds));
        uiStage.addActor(overlay);
    }

    @Override
    public void show() {
        dialogManager.addListener(this);
    }

    @Override
    public void hide() {
        dialogManager.removeListener(this);
    }

    // ---- DialogManager.DialogListener ----

    @Override
    public void onLine(String speaker, String text) {
        speakerLabel.setText(speaker == null ? "" : speaker);
        lineLabel.setText(text);
        choiceContainer.clear();
    }

    @Override
    public void onChoices(List<DialogChoice> choices) {
        choiceContainer.clear();
        for (int i = 0; i < choices.size(); i++) {
            final int index = i;
            TextButton button = new TextButton(choices.get(i).text, skin);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    dialogManager.selectChoice(index);
                }
            });
            choiceContainer.add(button).padTop(6).row();
        }
    }

    @Override
    public void onSceneChangeRequested(String sceneId) {
        // Ditangani terpusat oleh SceneManager (lihat GameMain), tidak perlu di sini.
    }

    @Override
    public void onDialogEnd() {
        // Opsional: subclass bisa override untuk sembunyikan UI dialog di titik ini.
    }

    // ---- ScreenAdapter ----

    @Override
    public void render(float delta) {
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        uiStage.dispose();
    }

    protected InputMultiplexer withUiStage(InputMultiplexer multiplexer) {
        multiplexer.addProcessor(uiStage);
        return multiplexer;
    }
}
