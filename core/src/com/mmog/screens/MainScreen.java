package com.mmog.screens;

import java.util.ArrayList;
import java.util.Random;

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


public class MainScreen extends AbstractScreen{
	TextButtonStyle tbs;
	boolean joined;
	Table table;
	Table table1;
	public static TextField playerName;
	SpriteBatch batch;
	Animation<TextureRegion> animation;
	float elapsedTime;

	public MainScreen() {
		// TODO Auto-generated constructor stub
		super();
	}

	public void show() {
		batch = new SpriteBatch();
		// TODO Auto-generated method stub
		Client.createPlayer("");
		
		if(Client.getPlayer() != null) {
			playerName.setText(Client.getPlayer().getPlayerName());
		}
		Gdx.input.setInputProcessor(this); 
	}


	public void buildStage() {
		tbs = new TextButtonStyle();
		//make fonts here
		BitmapFont font = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));
		BitmapFont gameTitleFont = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));
		BitmapFont labelFont = new BitmapFont(Gdx.files.internal("UI/newlabelfont.fnt"));

		//resizing fonts
		font.getData().setScale(0.1f);
		gameTitleFont.getData().setScale(0.4f);
		labelFont.getData().setScale(0.1f);
		
		//make label styles here
		LabelStyle ls = new LabelStyle(font, Color.MAGENTA);
		LabelStyle gameTitleFontLabel = new LabelStyle(gameTitleFont, Color.WHITE);
		LabelStyle labelFontStyle = new LabelStyle(labelFont, Color.RED);

		tbs.font = font;
		joined = false;

		//final TextureRegionDrawable bg = new TextureRegionDrawable(new Texture("UI/background2.png"));
		final TextureRegionDrawable textbox = new TextureRegionDrawable(new Texture("UI/textbox.png"));

		table = new Table();
		table.setFillParent(true);

		//make labels here
		Label playerNameLabel = new Label("Player Name", labelFontStyle);
		Label gameLabel = new Label("Between Us", gameTitleFontLabel);
		
		//make text button styles here
		TextButton.TextButtonStyle textbs = new TextButton.TextButtonStyle();
		textbs.font = font;
		textbs.fontColor = Color.GREEN;

		//make text button styles here
		TextButton.TextButtonStyle textbs2 = new TextButton.TextButtonStyle();
		textbs2.font = font;
		textbs2.fontColor = Color.WHITE;

		//make buttons here
		TextButton button1 = new TextButton("Join Room", textbs);
		TextButton button2 = new TextButton("Create Room", textbs);

		TextButton button3 = new TextButton("Settings",textbs2);
		TextButton button4 = new TextButton("About",textbs2);

		TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
		tfs.font = font;
		tfs.background = textbox;
		tfs.fontColor = new Color(Color.PURPLE);

		//table setting sizes
		float MAX_WIDTH = Gdx.graphics.getWidth();
		float MAX_HEIGTH = Gdx.graphics.getHeight();
		table.setSize(MAX_WIDTH, MAX_HEIGTH);

		playerName = new TextField("", tfs);
		playerName.setMaxLength(8);
		playerName.setAlignment(Align.center);

		//table items go here
		table.center().top();
		table.add(gameLabel).padLeft(30).padBottom(30);
		table.row();
		table.add(playerNameLabel).padBottom(5).padLeft(30);
		table.row();
		table.add(playerName).width(100).height(25).padBottom(25).padLeft(30);
		table.row().center();
		table.add(button1).width(90).padLeft(30).padBottom(25);
		table.row().center();
		table.add(button2).width(90).padLeft(30).padBottom(35);

		table.row();
		table.add(button3).padRight(200);
		table.add(button4);

		addActor(table);

		animation = createBackgroundAnimation(this.animation);

		//add button listeners here
		button1.addListener( new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Heading to Join Room Screen!");

				if(!playerName.getText().equals("") && !playerName.getText().contains(",")) {
					//create all the players and the local player
					Client.createPlayer(playerName.getText());
					Client.createPlayers();
				}
				
				if(Client.getPlayer() != null && !playerName.getText().equals("") && !playerName.getText().contains(",")) {
					ScreenManager.getInstance().showScreen(ScreenEnum.JOIN_SCREEN);
				}
			}
		});

		//create room button listener
		button2.addListener( new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Heading to Create Room Screen!");

				if(!playerName.getText().equals("") && !playerName.getText().contains(",")) {
					//create all the players and the local player
					Client.createPlayer(playerName.getText());
					Client.createPlayers();
				}
				
				if(Client.getPlayer() != null && !playerName.getText().equals("") && !playerName.getText().contains(",")) {
					ScreenManager.getInstance().showScreen(ScreenEnum.CREATE_ROOM);
				}
			}
		});

		//settings button listener
		button3.addListener( new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Heading to Settings from menu");
				ScreenManager.getInstance().showScreen(ScreenEnum.SETTINGS);
			}
		});

		//about button listener
		button4.addListener( new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Heading to About from menu");
				//ScreenManager.getInstance().showScreen(ScreenEnum.SETTINGS);=
			}
		});
		Gdx.input.setInputProcessor(this); 
	}

	public static Animation<TextureRegion> createBackgroundAnimation(Animation<TextureRegion> animation) {
		//animated background
		TextureRegion[] frames = new TextureRegion[50];

		//get all the frames
		for(int i = 0; i < 50; i++) {
			int j = i+1;
			frames[i] = (new TextureRegion(new Texture("bgFrames/" + j + ".jpg")));
		}

		animation = new Animation<TextureRegion>(1f/15f,frames);

		return animation;
	}

	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.input.setInputProcessor(this); 
		
		elapsedTime += Gdx.graphics.getDeltaTime();
		//System.out.println(elapsedTime);

		if(elapsedTime > 3f) {
			elapsedTime = 0f;
		}

		batch.begin();
		batch.draw(animation.getKeyFrame(elapsedTime),0,0);
		batch.end();

		draw();
	}

	public void resize(int width, int height) {
		// TODO Auto-generated method stub

	}

	public void pause() {
		// TODO Auto-generated method stub

	}

	public void resume() {
		// TODO Auto-generated method stub

	}


	public void hide() {
		// TODO Auto-generated method stub
		super.dispose();
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}

}