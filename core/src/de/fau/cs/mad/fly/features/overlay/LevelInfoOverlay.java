package de.fau.cs.mad.fly.features.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.fau.cs.mad.fly.Fly;
import de.fau.cs.mad.fly.features.IFeatureFinishLevel;
import de.fau.cs.mad.fly.features.IFeatureInit;
import de.fau.cs.mad.fly.features.IFeatureRender;
import de.fau.cs.mad.fly.game.GameController;

/**
 * Optional Feature to display a start and a finish message to the player.
 * 
 * @author Tobias Zangl
 */
public class LevelInfoOverlay implements IFeatureInit, IFeatureRender, IFeatureFinishLevel {
	private final Fly game;
	private GameController gameController;

	private Skin skin;
	private Stage stage;
	private Table table;
	
	private TextButton continueButton;
	
	public LevelInfoOverlay(final Fly game, Stage stage) {
		this.game = game;
		this.stage = stage;
		skin = game.getSkin();
		
		table = new Table();
		table.pad(Gdx.graphics.getWidth() * 0.2f);
		table.setFillParent(true);
		stage.addActor(table);
		
		final String infoString = "Level started!\n\nHave fun! :)";
		final Label infoLabel = new Label(infoString, skin);
		continueButton = new TextButton("GO!", skin, "default");
		
		final Table infoTable = new Table();
		final ScrollPane pane = new ScrollPane(infoTable, skin);
		infoTable.add(infoLabel).pad(10f);
		infoTable.row();
		infoTable.add(continueButton).pad(10f);
		pane.setFadeScrollBars(true);
		
		table.row().expand();
		table.add(pane);
	}

	@Override
	public void render(float delta) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GameController gameController) {
		this.gameController = gameController;
		continueButton.addListener(new ClickListener() {
			@Override 
			public void clicked(InputEvent event, float x, float y) {
				game.gameController.startGame();
			}
		});
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

}