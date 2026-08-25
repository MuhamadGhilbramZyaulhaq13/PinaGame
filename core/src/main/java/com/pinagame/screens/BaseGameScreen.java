package com.pinagame.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.pinagame.core.DialogManager;
import com.pinagame.core.dialog.DialogChoice;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.List;


public abstract class BaseGameScreen extends ScreenAdapter implements DialogManager.DialogListener {

    protected final Game game;
    protected final DialogManager dialogManager;
    protected final String sceneId;

    protected Stage uiStage;
    protected Skin skin;
    protected Table dialogueBox;
    protected Label speakerLabel;
    protected Label lineLabel;
    protected Table choiceContainer;

    public BaseGameScreen(Game game, DialogManager dialogManager, String sceneId) {
        this.game = game;
        this.dialogManager = dialogManager;
        this.sceneId = sceneId;
    }
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

        Actor clickCatcher = new Actor();
        clickCatcher.setBounds(-10000, -10000, 20000, 20000);
        clickCatcher.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialogManager.advance();
            }
        });

        uiStage.addActor(clickCatcher);
        uiStage.addActor(dialogueBox);
        Gdx.input.setInputProcessor(uiStage);
    }

    protected Skin loadSkin() {
        return new Skin(com.badlogic.gdx.Gdx.files.internal("ui/uiskin.json"));
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
    }

    @Override
    public void onDialogEnd() {
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
