package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.pinagame.core.DialogManager;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import java.util.ArrayList;
import java.util.List;


public class SocialMediaScreen extends BaseGameScreen {

    public enum UISubScreen { HOME_FEED, SEARCH, PROFILE_WRONG, PROFILE_CORRECT, CHAT, POST_SCREENSHOT }

    private UISubScreen currentSubScreen;
    private TextButton wrongFollowButton;
    private final List<Texture> generatedTextures = new ArrayList<>();

    public SocialMediaScreen(Game game, DialogManager dialogManager, String sceneId) {
        super(game, dialogManager, sceneId);
        this.currentSubScreen = resolveSubScreen(sceneId);
    }

    private UISubScreen resolveSubScreen(String sceneId) {
        if (sceneId == null) return UISubScreen.HOME_FEED;
        if (sceneId.contains("PROFILE_WRONG")) return UISubScreen.PROFILE_WRONG;
        if (sceneId.contains("PROFILE_CORRECT")) return UISubScreen.PROFILE_CORRECT;
        if (sceneId.contains("CHAT")) return UISubScreen.CHAT;
        if (sceneId.contains("POST_SCREENSHOT")) return UISubScreen.POST_SCREENSHOT;
        if (sceneId.contains("SEARCH")) return UISubScreen.SEARCH;
        return UISubScreen.HOME_FEED;
    }

    @Override
    public void show() {
        super.show();
        buildDialogueUI();
        buildPhoneUI();

    }

    @Override
    public void render(float delta) {
        clearScreen();
        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Texture t : generatedTextures) t.dispose();
    }

    // ---------------------------------------------------------------------
    // Bezel HP + status bar
    // ---------------------------------------------------------------------

    private void buildPhoneUI() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float phoneW = Math.min(380f, screenW * 0.42f);
        float phoneH = Math.min(680f, screenH * 0.78f);
        float phoneX = (screenW - phoneW) / 2f;
        float phoneY = screenH - phoneH - 56f;

        Table bezel = new Table();
        bezel.setBackground(solidDrawable(new Color(0.08f, 0.08f, 0.08f, 1f)));
        bezel.setSize(phoneW, phoneH);
        bezel.setPosition(phoneX, phoneY);
        bezel.top();

        Table statusBar = new Table();
        Label clock = new Label("09:41", skin);
        clock.setColor(Color.WHITE);
        tapAdvances(clock);
        Label battery = new Label("100%", skin);
        battery.setColor(Color.WHITE);
        tapAdvances(battery);
        statusBar.add(clock).expandX().left().padLeft(14);
        statusBar.add(battery).padRight(14);
        tapAdvances(statusBar);

        Table screenArea = new Table();
        screenArea.setBackground(solidDrawable(Color.WHITE));
        screenArea.top();
        screenArea.add(buildSubScreenContent()).growX().top();
        tapAdvances(screenArea);

        bezel.add(statusBar).growX().height(26).row();
        bezel.add(screenArea).grow().pad(6);
        tapAdvances(bezel);

        uiStage.addActor(bezel);
    }

    // ---------------------------------------------------------------------
    // Konten per sub-layar
    // ---------------------------------------------------------------------

    private Table buildSubScreenContent() {
        switch (currentSubScreen) {
            case SEARCH: return buildSearchScreen();
            case PROFILE_WRONG: return buildProfileScreen(false);
            case PROFILE_CORRECT: return buildProfileScreen(true);
            case CHAT: return buildChatScreen();
            case POST_SCREENSHOT: return buildPostScreenshotScreen();
            case HOME_FEED:
            default: return buildHomeFeedScreen();
        }
    }
    private Table buildPostScreenshotScreen() {
        Table content = new Table();
        content.top().pad(10);
        tapAdvances(content);

        // Header ala postingan IG: avatar + username
        Table header = new Table();
        Image avatar = avatarPlaceholder(28, new Color(0.65f, 0.55f, 0.9f, 1f));
        tapAdvances(avatar);
        header.add(avatar).size(28).padRight(8);
        Label username = new Label("mondu.ck", skin);
        username.setColor(Color.BLACK);
        tapAdvances(username);
        header.add(username).left();
        tapAdvances(header);
        content.add(header).left().growX().padBottom(8).row();

        // "Foto"-nya: screenshot chat in-game, latar HIJAU ala kebun biar kerasa
        // ini beneran diambil dari dalam game, bukan kotak abu-abu polos.
        Table photoFrame = new Table();
        photoFrame.setBackground(solidDrawable(new Color(0.42f, 0.62f, 0.34f, 1f)));
        photoFrame.pad(10);
        tapAdvances(photoFrame);

        Table bubble1 = new Table();
        bubble1.setBackground(solidDrawable(new Color(0.97f, 0.97f, 0.97f, 0.95f)));
        Label q = new Label("Berapa akar dari 1444?", skin);
        q.setColor(Color.BLACK);
        tapAdvances(q);
        bubble1.add(q).pad(6);
        tapAdvances(bubble1);
        photoFrame.add(bubble1).left().padBottom(6).row();

        Table bubble2 = new Table();
        bubble2.setBackground(solidDrawable(new Color(0.6f, 0.8f, 0.98f, 0.95f)));
        Label a = new Label("Hmm... 38! Gampang itu mah.", skin);
        a.setColor(Color.BLACK);
        tapAdvances(a);
        bubble2.add(a).pad(6);
        tapAdvances(bubble2);
        photoFrame.add(bubble2).right().row();

        content.add(photoFrame).growX().height(140).padBottom(8).row();

        // Baris ikon versi teks (aman dari font yang gak dukung emoji)
        Table iconRow = new Table();
        Label likeIcon = new Label("[suka]", skin);
        likeIcon.setColor(Color.DARK_GRAY);
        tapAdvances(likeIcon);
        Label commentIcon = new Label("[komentar]", skin);
        commentIcon.setColor(Color.DARK_GRAY);
        tapAdvances(commentIcon);
        Label shareIcon = new Label("[bagikan]", skin);
        shareIcon.setColor(Color.DARK_GRAY);
        tapAdvances(shareIcon);
        iconRow.add(likeIcon).padRight(12);
        iconRow.add(commentIcon).padRight(12);
        iconRow.add(shareIcon);
        tapAdvances(iconRow);
        content.add(iconRow).left().padBottom(6).row();

        Label likeCount = new Label("24 suka", skin);
        likeCount.setColor(Color.BLACK);
        tapAdvances(likeCount);
        content.add(likeCount).left();

        return content;
    }

    private Table buildSearchScreen() {
        boolean isWrongSearch = sceneId != null && sceneId.contains("WRONG");
        String displayedQuery = isWrongSearch ? "safinamm" : "safinamn";

        Table content = new Table();
        content.top().pad(10);
        tapAdvances(content);

        Table searchBar = new Table();
        searchBar.setBackground(solidDrawable(new Color(0.92f, 0.92f, 0.92f, 1f)));
        Label queryLabel = new Label("Cari:  " + displayedQuery, skin);
        queryLabel.setColor(Color.BLACK);
        tapAdvances(queryLabel);
        searchBar.add(queryLabel).left().pad(8);
        tapAdvances(searchBar);
        content.add(searchBar).growX().height(36).padBottom(14).row();

        content.add(buildSearchResultRow(displayedQuery, isWrongSearch)).growX().padBottom(10).row();
        if (!isWrongSearch) {
            content.add(buildSearchResultRow("safinamn_official", false)).growX().padBottom(10).row();
        }

        return content;
    }

    private Table buildSearchResultRow(String username, boolean privateLock) {
        Table row = new Table();
        Image avatar = avatarPlaceholder(40, new Color(0.7f, 0.7f, 0.75f, 1f));
        tapAdvances(avatar);
        row.add(avatar).size(40).padRight(10);

        Table textCol = new Table();
        Label nameLabel = new Label(username, skin);
        nameLabel.setColor(Color.BLACK);
        tapAdvances(nameLabel);
        textCol.add(nameLabel).left().row();
        if (privateLock) {
            Label lock = new Label("[Akun privat]", skin);
            lock.setColor(Color.GRAY);
            tapAdvances(lock);
            textCol.add(lock).left();
        }
        tapAdvances(textCol);
        row.add(textCol).left().growX();
        tapAdvances(row);
        return row;
    }

    private Table buildProfileScreen(boolean correct) {
        Table content = new Table();
        content.top().pad(14);
        tapAdvances(content);

        Color avatarColor = correct
            ? new Color(0.65f, 0.55f, 0.9f, 1f)
            : new Color(0.7f, 0.7f, 0.75f, 1f);
        Image avatar = avatarPlaceholder(84, avatarColor);
        tapAdvances(avatar);
        content.add(avatar).size(84).padBottom(8).row();

        Label username = new Label(correct ? "safinanm" : "safinamm", skin);
        username.setColor(Color.BLACK);
        tapAdvances(username);
        content.add(username).padBottom(4).row();

        if (!correct) {
            Label lock = new Label("[Akun ini privat]", skin);
            lock.setColor(Color.GRAY);
            tapAdvances(lock);
            content.add(lock).padBottom(4).row();
        }

        Label followers = new Label(correct ? "875 pengikut" : "12.483 pengikut", skin);
        followers.setColor(Color.DARK_GRAY);
        tapAdvances(followers);
        content.add(followers).padBottom(14).row();

        TextButton followBtn = new TextButton(correct ? "Following" : "Fsollow", skin);
        if (correct) {
            tapAdvances(followBtn);
        } else {
            setupWrongProfileFollowButton(followBtn);
        }
        content.add(followBtn).width(140).height(32).padBottom(16).row();

        if (correct) {
            Table grid = new Table();
            tapAdvances(grid);
            for (int i = 0; i < 6; i++) {
                Color c = new Color(0.5f + (i % 3) * 0.1f, 0.6f, 0.8f - (i % 2) * 0.1f, 1f);
                Image tile = squarePlaceholder(52, c);
                tapAdvances(tile);
                grid.add(tile).size(52).pad(2);
                if (i % 3 == 2) grid.row();
            }
            content.add(grid).row();
        } else {
            Label lockedGrid = new Label("[Ikuti akun ini untuk melihat foto dan videonya]", skin);
            lockedGrid.setColor(Color.GRAY);
            lockedGrid.setWrap(true);
            tapAdvances(lockedGrid);
            content.add(lockedGrid).width(220).row();
        }

        return content;
    }

    private Table buildChatScreen() {
        // TODO: belum dipakai di Chapter 1 saat ini, disiapkan untuk chapter mendatang.
        Table content = new Table();
        content.top().pad(10);
        Label placeholder = new Label("(chat)", skin);
        placeholder.setColor(Color.GRAY);
        tapAdvances(placeholder);
        content.add(placeholder);
        tapAdvances(content);
        return content;
    }

    private Table buildHomeFeedScreen() {
        Table content = new Table();
        content.top().pad(10);
        Label placeholder = new Label("Instagram", skin);
        placeholder.setColor(Color.BLACK);
        tapAdvances(placeholder);
        content.add(placeholder);
        tapAdvances(content);
        return content;
    }

    // ---------------------------------------------------------------------
    // Utilitas: grafis placeholder & "klik dimana saja tetap lanjut dialog"
    // ---------------------------------------------------------------------

    private Drawable solidDrawable(Color color) {
        return skin.newDrawable("white", color);
    }

    private Texture createCircleTexture(int diameter, Color color) {
        Pixmap pm = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fillCircle(diameter / 2, diameter / 2, diameter / 2);
        Texture tex = new Texture(pm);
        pm.dispose();
        generatedTextures.add(tex);
        return tex;
    }

    private Texture createSquareTexture(int size, Color color) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        generatedTextures.add(tex);
        return tex;
    }

    private Image avatarPlaceholder(int diameter, Color color) {
        return new Image(createCircleTexture(diameter, color));
    }

    private Image squarePlaceholder(int size, Color color) {
        return new Image(createSquareTexture(size, color));
    }



    private void tapAdvances(Actor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialogManager.advance();
            }
        }
        );
    }
    private static final Color FOLLOW_NEUTRAL = new Color(0.6f, 0.6f, 0.6f, 1f);
    private static final Color FOLLOW_ACTIVE_BLUE = new Color(0.45f, 0.7f, 0.95f, 1f);
    private static final Color FOLLOW_CLICKED_GRAY = new Color(0.92f, 0.92f, 0.92f, 1f);

    /**
     * Tombol Follow di profil yang SALAH: nonaktif & abu-abu netral dulu (gak bisa
     * diklik), baru aktif + biru muda begitu dialog nyampe baris Heri ("Gapapa
     * bray") -- lewat activateWrongFollowButton() yang dipanggil dari onLine().
     * Setelah itu, tombol ini jadi SATU-SATUNYA cara lanjut (node WAIT_FOLLOW di
     * JSON nge-block tap/klik biasa secara mutlak). Diklik -> warna abu-abu nyaris
     * putih, dialog lanjut lewat continueFromPause().
     */
    private void setupWrongProfileFollowButton(TextButton button) {
        button.setColor(FOLLOW_NEUTRAL);
        button.setDisabled(true);
        button.setTouchable(Touchable.disabled);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                button.setColor(FOLLOW_CLICKED_GRAY);
                button.setDisabled(true);
                button.setTouchable(Touchable.disabled);
                dialogManager.forceAdvance();            }
        });

        wrongFollowButton = button;
    }
    @Override
    public void onLine(String speaker, String text) {
        super.onLine(speaker, text);
        if (currentSubScreen == UISubScreen.PROFILE_WRONG && "Heri".equals(speaker)) {
            activateWrongFollowButton();
            dialogManager.blockAdvanceUntilExternalTrigger();
        }
    }

    private void activateWrongFollowButton() {
        if (wrongFollowButton == null) return;
        wrongFollowButton.setColor(FOLLOW_ACTIVE_BLUE);
        wrongFollowButton.setDisabled(false);
        wrongFollowButton.setTouchable(Touchable.enabled);
    }
}
