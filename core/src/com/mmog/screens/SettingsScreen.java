package com.mmog.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.mmog.Client;
import com.mmog.GameConfig;

/**
 * Settings screen for configuring server connection options.
 */
public class SettingsScreen extends AbstractScreen {
    Table table;
    SpriteBatch batch;
    Animation<TextureRegion> animation;
    float elapsedTime;

    TextField relayUrlField;
    TextField serverHostField;
    TextField serverPortField;
    CheckBox useRelayCheckbox;

    public SettingsScreen() {
        super();
    }

    public void show() {
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(this);
    }

    public void buildStage() {
        // Fonts
        BitmapFont font = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));
        BitmapFont titleFont = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));
        font.getData().setScale(0.1f);
        titleFont.getData().setScale(0.25f);

        // Styles
        LabelStyle labelStyle = new LabelStyle(font, Color.WHITE);
        LabelStyle titleStyle = new LabelStyle(titleFont, Color.CYAN);

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.GREEN;

        TextButtonStyle backButtonStyle = new TextButtonStyle();
        backButtonStyle.font = font;
        backButtonStyle.fontColor = Color.RED;

        final TextureRegionDrawable textbox = new TextureRegionDrawable(new Texture("UI/textbox.png"));

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.background = textbox;
        textFieldStyle.fontColor = Color.WHITE;

        // Create checkbox style
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        // Simple checkbox visuals using text
        checkBoxStyle.checkboxOn = textbox;
        checkBoxStyle.checkboxOff = textbox;

        // Table setup
        table = new Table();
        table.setFillParent(true);
        table.center().top().padTop(50);

        // Title
        Label titleLabel = new Label("Settings", titleStyle);
        table.add(titleLabel).colspan(2).padBottom(40);
        table.row();

        // Connection Mode
        Label modeLabel = new Label("Connection Mode", labelStyle);
        table.add(modeLabel).padRight(20).padBottom(10);

        useRelayCheckbox = new CheckBox(" Use Relay Server (Recommended)", checkBoxStyle);
        useRelayCheckbox.setChecked(GameConfig.useRelay());
        table.add(useRelayCheckbox).left().padBottom(10);
        table.row();

        // Relay URL
        Label relayLabel = new Label("Relay Server URL", labelStyle);
        table.add(relayLabel).padRight(20).padBottom(10);

        relayUrlField = new TextField(GameConfig.getRelayUrl(), textFieldStyle);
        relayUrlField.setAlignment(Align.center);
        table.add(relayUrlField).width(300).height(25).padBottom(10);
        table.row();

        // Separator
        Label directLabel = new Label("-- Direct Connection (Advanced) --", labelStyle);
        table.add(directLabel).colspan(2).padTop(20).padBottom(10);
        table.row();

        // Server Host
        Label hostLabel = new Label("Server Host/IP", labelStyle);
        table.add(hostLabel).padRight(20).padBottom(10);

        serverHostField = new TextField(GameConfig.getServerHost(), textFieldStyle);
        serverHostField.setAlignment(Align.center);
        table.add(serverHostField).width(200).height(25).padBottom(10);
        table.row();

        // Server Port
        Label portLabel = new Label("Server Port", labelStyle);
        table.add(portLabel).padRight(20).padBottom(10);

        serverPortField = new TextField(String.valueOf(GameConfig.getServerPort()), textFieldStyle);
        serverPortField.setAlignment(Align.center);
        table.add(serverPortField).width(100).height(25).padBottom(10);
        table.row();

        // Buttons
        Table buttonTable = new Table();

        TextButton saveButton = new TextButton("Save", buttonStyle);
        TextButton backButton = new TextButton("Back", backButtonStyle);

        buttonTable.add(saveButton).width(80).padRight(30);
        buttonTable.add(backButton).width(80);

        table.add(buttonTable).colspan(2).padTop(40);
        table.row();

        // Status label
        final Label statusLabel = new Label("", labelStyle);
        table.add(statusLabel).colspan(2).padTop(20);

        addActor(table);

        // Background animation
        animation = MainScreen.createBackgroundAnimation(this.animation);

        // Button listeners
        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Save settings
                GameConfig.setUseRelay(useRelayCheckbox.isChecked());
                GameConfig.setRelayUrl(relayUrlField.getText().trim());
                GameConfig.setServerHost(serverHostField.getText().trim());

                try {
                    int port = Integer.parseInt(serverPortField.getText().trim());
                    GameConfig.setServerPort(port);
                } catch (NumberFormatException e) {
                    statusLabel.setText("Invalid port number!");
                    statusLabel.setColor(Color.RED);
                    return;
                }

                GameConfig.saveConfig();
                statusLabel.setText("Settings saved! Restart game to apply.");
                statusLabel.setColor(Color.GREEN);
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ScreenManager.getInstance().showScreen(ScreenEnum.MAIN_MENU);
            }
        });

        Gdx.input.setInputProcessor(this);
    }

    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.input.setInputProcessor(this);

        elapsedTime += Gdx.graphics.getDeltaTime();
        if (elapsedTime > 3f) {
            elapsedTime = 0f;
        }

        batch.begin();
        batch.draw(animation.getKeyFrame(elapsedTime), 0, 0);
        batch.end();

        draw();
    }

    public void resize(int width, int height) {}
    public void pause() {}
    public void resume() {}

    public void hide() {
        super.dispose();
    }

    public void dispose() {}
}
