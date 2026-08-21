package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.pinagame.core.DialogManager;
import com.pinagame.core.dialog.DialogChoice;

import java.util.List;

/**
 * Simulasi antarmuka smartphone (Instagram-like) sebagai overlay penuh layar
 * (mis. dibingkai frame HP biar jelas ini "layar dalam layar").
 *
 * State sub-layar (search / profile salah / profile benar / chat) ditentukan dari
 * sceneId yang dikirim SceneManager — bukan hardcode di kode, supaya penulis cerita
 * bisa atur alur "salah follow akun -> ketemu yang benar" cukup lewat JSON.
 */
public class SocialMediaScreen extends BaseGameScreen {

    public enum UISubScreen { HOME_FEED, SEARCH, PROFILE_WRONG, PROFILE_CORRECT, CHAT }

    private UISubScreen currentSubScreen;

    public SocialMediaScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
        this.currentSubScreen = resolveSubScreen(sceneId);
    }

    private UISubScreen resolveSubScreen(String sceneId) {
        if (sceneId == null) return UISubScreen.HOME_FEED;
        if (sceneId.contains("PROFILE_WRONG")) return UISubScreen.PROFILE_WRONG;
        if (sceneId.contains("PROFILE_CORRECT")) return UISubScreen.PROFILE_CORRECT;
        if (sceneId.contains("CHAT")) return UISubScreen.CHAT;
        if (sceneId.contains("SEARCH")) return UISubScreen.SEARCH;
        return UISubScreen.HOME_FEED;
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        // Sama seperti PixelArtScreen: screen ini selalu dimasuki di tengah alur dialog
        // yang sedang berjalan lewat CHANGE_SCENE, jadi tidak perlu startFrom() manual di sini.
    }

    @Override
    public void onChoices(List<DialogChoice> choices) {
        super.onChoices(choices);
        // Tempat menambah tombol UI bertema (mis. tombol "Follow" biru ala Instagram)
        // kalau ingin choice direpresentasikan sebagai elemen UI HP, bukan tombol teks polos.
    }

    @Override
    public void render(float delta) {
        clearScreen();
        switch (currentSubScreen) {
            case SEARCH:
                // TODO: gambar search bar + daftar hasil pencarian "safinamn"
                // (termasuk akun private 12k followers yang salah)
                break;
            case PROFILE_WRONG:
                // TODO: gambar profile card akun private yang salah (12k followers, terkunci)
                break;
            case PROFILE_CORRECT:
                // TODO: gambar profile card akun "safinamn" yang benar
                break;
            case CHAT:
                // TODO: gambar bubble chat (Datt <-> Heri, atau notifikasi DM)
                break;
            case HOME_FEED:
            default:
                // TODO: gambar tampilan feed/home default
                break;
        }

        // TODO: bungkus semua render di atas dengan frame bezel HP supaya terasa
        // seperti "layar dalam layar", bedakan dari GARDEN_3D/PIXEL_ART_2D.

        super.render(delta);
    }
}
