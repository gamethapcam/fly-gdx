package de.fau.cs.mad.fly.player;

import de.fau.cs.mad.fly.I18n;
import de.fau.cs.mad.fly.res.Level;
import de.fau.cs.mad.fly.settings.SettingManager;

/**
 * Stores all player-specific information.
 * 
 * @author Lukas Hahmann
 *
 */
public class Player {
	
	/** The plane the player is currently steering */	
	private IPlane plane;
	
	private Level.Head lastLevel;
	private Level level;
	private String name;
	private int id;

	/** The lives the player has at the moment. If lives is lower or equal zero the player is dead. */
	private int lives;

	private SettingManager settingManager;
	
	/**
	 * Getter for the name.
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter for the name.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Getter for the ID.
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Setter for the ID.
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Getter for the lives of the player.
	 * @return lives
	 */
	public int getLives() {
		return lives;
	}
	
	/**
	 * Decreases the live by 1 if the player has enough lives, otherwise he has 0 lives left.
	 * @return true, if lifes can be decreased (>1) false otherise
	 */
	public boolean decreaseLives() {
		if(lives > 1) {
			lives--;
			return true;
		} else {
			lives = 0;
			return false;
		}
	}
	
	/**
	 * Returns if the player is dead or alive.
	 * @return true if the player is dead because he has 0 lives left, false otherwise.
	 */
	public boolean isDead() {
		if(lives > 0)
			return true;
		return false;
	}
	
	/**
	 * Setter for the lives.
	 * @param lives
	 */
	public void setLives(int lives) {
		this.lives = lives;
	}
	
	/**
	 * Creates a new player without any more information.
	 */
	public Player() {		
		this.plane = new Spaceship("spaceship");
	}
	
	/**
	 * Creates a new player.
	 * @param name		Name of the player.
	 * @param id		ID of the player.
	 */
	public Player(String name, int id) {
		setName(name);
		setId(id);
		setLives(1);
		this.plane = new Spaceship("spaceship");
	}

	/**
	 * Getter for the last level the player has played.
	 * @return lastLevel
	 */
	public Level.Head getLastLevel() {
		return lastLevel;
	}

	/**
	 * Setter for the last level the player has played.
	 * @param lastLevel
	 */
	public void setLastLevel(Level.Head lastLevel) {
		this.lastLevel = lastLevel;
	}

	/**
	 * Setter for the current level the player is playing.
	 * @param l
	 */
	public void setLevel(Level l) { this.level = l; }

	/**
	 * Getter for the current level the player is playing.
	 * @return level
	 */
	public Level getLevel() { return level; }

	/**
	 * Getter for the plane of the player.
	 * @return plane
	 */
	public IPlane getPlane() {
		return plane;
	}

	/**
	 * Setter for the plane of the player.
	 * @param plane
	 */
	public void setPlane(IPlane plane) {
		this.plane = plane;
	}
	
	/**
	 * Creates the SettingManager and all the Settings.
	 */
	public void createSettings() {
		settingManager = new SettingManager("fly_user_preferences_" + getId());

		settingManager.addBooleanSetting(SettingManager.USE_TOUCH, I18n.t("use.touch"), false);
		settingManager.addBooleanSetting(SettingManager.USE_ROLL_STEERING, I18n.t("use.rolling"), false);
		// removed for release: settingManager.addBooleanSetting(SettingManager.USE_LOW_PASS_FILTER, "Use LowPassFilter:", false);
		// removed for release: settingManager.addBooleanSetting(SettingManager.SHOW_GATE_INDICATOR, "Show next Gate:", true);
		settingManager.addBooleanSetting(SettingManager.SHOW_PAUSE, I18n.t("show.pause"), false);
		settingManager.addBooleanSetting(SettingManager.SHOW_STEERING, I18n.t("show.steering"), false);
		// removed for release: settingManager.addBooleanSetting(SettingManager.SHOW_FPS, "Show FPS:", false);
		// removed for release: settingManager.addBooleanSetting(SettingManager.FIRST_PERSON, "First Person", false);
		
		// removed for release: settingManager.addFloatSetting(SettingManager.ALPHA_SLIDER, "Alpha:", 15.0f, 0.0f, 100.0f, 1.0f);
		// removed for release: settingManager.addFloatSetting(SettingManager.BUFFER_SLIDER, "Buffersize:", 30.0f, 0.0f, 100.0f, 1.0f);
		// removed for release: settingManager.addFloatSetting(SettingManager.CAMERA_OFFSET, "Camera Distance:", 50.0f, 0.0f, 100.0f, 1.0f);
	}
	
	/**
	 * Getter for the SettingManager.
	 */
	public SettingManager getSettingManager() {
		return settingManager;
	}
}
