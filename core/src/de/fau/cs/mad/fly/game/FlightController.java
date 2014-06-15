package de.fau.cs.mad.fly.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import de.fau.cs.mad.fly.player.Player;

import java.util.ArrayList;
import java.util.List;

public class FlightController implements InputProcessor {

	private boolean useSensorData;
	private boolean useRolling;
	private boolean useLowPass;
	private boolean useAveraging;

	private Player player;
	private PerspectiveCamera camera;
	private float cameraOffset;

	private float startRoll, startAzimuth;

	private float rollDir = 0.0f;
	private float azimuthDir = 0.0f;

	private int currentEvent = -1;
	
	private float screenHeight = Gdx.graphics.getHeight();
	private float screenWidth = Gdx.graphics.getWidth();

	// variables for Sensor input smoothing
	private float alpha = 0.15f;
	private int bufferSize;
	private List<Float> rollInput;
	private List<Float> rollOutput;
	private List<Float> pitchInput;
	private List<Float> pitchOutput;
	private List<Float> azimuthInput;
	private List<Float> azimuthOutput;

	public FlightController(Player player) {
		this.player = player;

		this.useSensorData = !player.getSettingManager().getCheckBoxValue("useTouch");
		this.useRolling = player.getSettingManager().getCheckBoxValue("useRoll");
		this.useLowPass = player.getSettingManager().getCheckBoxValue("useLowPass");
		this.useAveraging = player.getSettingManager().getCheckBoxValue("useAveraging");

		this.bufferSize = (int) player.getSettingManager().getSliderValue("bufferSlider");
		this.alpha = player.getSettingManager().getSliderValue("alphaSlider") / 100.f;
		this.cameraOffset = player.getSettingManager().getSliderValue("cameraOffset") / 100.f;

		setUpCamera();
	}

	public PerspectiveCamera getCamera() {
		return camera;
	}

	public void setUseSensorData(boolean useSensorData) {
		this.useSensorData = useSensorData;
	}

	public void setUseRolling(boolean useRolling) {
		this.useRolling = useRolling;
	}

	public void setUseLowPass(boolean useLowPass) {
		this.useLowPass = useLowPass;
	}

	public void setUseAveraging(boolean useAveraging) {
		this.useAveraging = useAveraging;
	}

	public void setBufferSize(int bufferSize) {
		resetBuffers();

		this.bufferSize = bufferSize;

	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public float getRollDir() {
		return rollDir;
	}

	public float getAzimuthDir() {
		return azimuthDir;
	}
	
	public void resetSteering() {
		float roll = Gdx.input.getRoll();
		float pitch = Gdx.input.getPitch();
		float azimuth = Gdx.input.getAzimuth();

		Gdx.app.log("FlightController.resetSteering", "roll=" + roll + " pitch=" + pitch + " azimuth=" + azimuth);
		startAzimuth = computeAzimuth(roll, pitch, azimuth);
		Gdx.app.log("FlightController.resetSteering", "roll=" + roll + " pitch=" + pitch + " azimuth=" + azimuth);
		startRoll = Gdx.input.getRoll();
	}

	/**
	 * recomputes camera position and rotation
	 * 
	 * @param delta
	 *            - time since last frame
	 * @return
	 */
	public PerspectiveCamera recomputeCamera(float delta) {
		// rotating the camera according to UserInput
		if (useSensorData) {
			interpretSensorInput();
		}
		
		/*Vector3 dir = new Vector3(camera.direction.x, camera.direction.y, camera.direction.z);
		camera.translate(dir.scl(player.getPlane().getSpeed() * cameraOffset));*/

		// rotating the camera
		rotateCamera(rollDir, azimuthDir);
		camera.update();

		// move the camera (first person flight)
		Vector3 dir = new Vector3(camera.direction.x, camera.direction.y, camera.direction.z);
		camera.translate(dir.scl(player.getPlane().getSpeed() * delta));//(delta - cameraOffset)));
		camera.update();
		
		return camera;
	}

	/**
	 * Sets up the camera for the initial view.
	 */
	public final void setUpCamera() {

		// initializing Roll- and Pitch-Values for later comparison
		resetSteering();

		// setting up the camera
		camera = new PerspectiveCamera(67, screenWidth, screenHeight);

		camera.position.set(player.getLevel().start.position);
		camera.lookAt(player.getLevel().start.viewDirection);
		camera.near = 0.1f;
		// within a sphere it should not happen that not everything of this
		// sphere is displayed. Therefore use the diameter as far plane
		camera.far = player.getLevel().radius * 2;
		camera.update();

		resetBuffers();
	}

	private void resetBuffers() {
		rollInput = new ArrayList<Float>();
		rollOutput = new ArrayList<Float>();
		pitchInput = new ArrayList<Float>();
		pitchOutput = new ArrayList<Float>();
		azimuthInput = new ArrayList<Float>();
		azimuthOutput = new ArrayList<Float>();
	}
	
	float maxRotate = 45.f;

	/**
	 * Interprets the rotation of the smartphone and calls camera rotation
	 */
	private void interpretSensorInput() {
		float roll = Gdx.input.getRoll();
		float pitch = Gdx.input.getPitch();
		float azimuth = Gdx.input.getAzimuth();

		// Gdx.app.log("myApp", "roll: " + roll + "; pitch: " + pitch +
		// "; azimuth: " + azimuth);

		// removing oldest element in buffers
		if (rollInput.size() >= bufferSize) {
			rollInput.remove(0);
			pitchInput.remove(0);
			azimuthInput.remove(0);
		}

		// adding newest sensor-data to buffers
		rollInput.add(roll);
		pitchInput.add(pitch);
		azimuthInput.add(azimuth);

		if (useLowPass) {
			rollOutput = lowPassFilter(rollInput, rollOutput, alpha);
			pitchOutput = lowPassFilter(pitchInput, pitchOutput, alpha);
			azimuthOutput = lowPassFilter(azimuthInput, azimuthOutput, alpha);

			if (useAveraging) {
				roll = average(rollOutput);
				pitch = average(pitchOutput);
				azimuth = average(azimuthOutput);
			}
		} else {
			if (useAveraging) {
				roll = average(rollInput);
				pitch = average(pitchInput);
				azimuth = average(azimuthInput);
			}
		}

		azimuth = computeAzimuth(roll, pitch, azimuth);

		float difRoll = roll - startRoll;
		if (Math.abs(difRoll) > 180) {
			difRoll -= Math.signum(difRoll) * 360;
		}

		float difAzimuth = azimuth - startAzimuth;
		if (Math.abs(difAzimuth) > 180) {
			difAzimuth -= Math.signum(difAzimuth) * 360;
		}

		// capping the rotation to a maximum of 90 degrees
		if (Math.abs(difRoll) > maxRotate) {
			difRoll = maxRotate * Math.signum(difRoll);
		}

		if (Math.abs(difAzimuth) > maxRotate) {
			difAzimuth = maxRotate * Math.signum(difAzimuth);
		}

		rollDir = 0.0f;
		azimuthDir = 0.0f;

		// camera rotation according to smartphone rotation
		setAzimuthDir(difAzimuth / -maxRotate);
		setRollDir(difRoll / -maxRotate);
	}

	/**
	 * Setter for the {@link #azimuthDir}. Values greater than the azimuthSpeed
	 * of the plane are reduced to the azimuth speed of the plane.
	 * 
	 * @param azimuthDir
	 */
	private void setAzimuthDir(float azimuthDir) {
		this.azimuthDir = limitSpeed(azimuthDir, player.getPlane().getAzimuthSpeed());
	}

	/**
	 * Setter for the {@link #rollDir}. Values greater than the rollingSpeed of
	 * the plane are reduced to the azimuth speed of the plane.
	 * 
	 * @param rollDir
	 */
	private void setRollDir(float rollDir) {
		this.rollDir = limitSpeed(rollDir, player.getPlane().getRollingSpeed());
	}

	/**
	 * Rotates the camera according to rollDir and pitchDir
	 * 
	 * @param rollDir
	 *            - defines if the camera should be rotated up or down
	 * @param azimuthDir
	 *            - defines if the camera should be rotated left or right
	 */
	private void rotateCamera(float rollDir, float azimuthDir) {
		// rotation up or down
		camera.rotate(camera.direction.cpy().crs(camera.up), 1.0f * rollDir);

		// rotation around camera.direction/viewDirection (roll)
		if (useRolling) {
			camera.rotate(camera.direction, 1.0f * -azimuthDir);
		} else {
			// rotation around camera.up (turning left/right)
			camera.rotate(camera.up, 1.0f * azimuthDir);
		}
	}

	/**
	 * computes the rotation around z-Axis relative to the smartphone
	 * 
	 * @param roll
	 * @param pitch
	 * @param azimuth
	 * @return
	 */
	private float computeAzimuth(float roll, float pitch, float azimuth) {
		Matrix3 mX = new Matrix3();
		Matrix3 mY = new Matrix3();
		Matrix3 mZ = new Matrix3();

		roll = roll * (float) Math.PI / 180.f;
		pitch = pitch * (float) Math.PI / 180.f;
		azimuth = azimuth * (float) Math.PI / 180.f;

		float cos = (float) Math.cos(pitch);
		float sin = (float) Math.sin(pitch);

		float[] values = { 1.f, 0.f, 0.f, 0.f, cos, sin, 0.f, -sin, cos };
		mY.set(values);

		cos = (float) Math.cos(roll);
		sin = (float) Math.sin(roll);
		float[] values2 = { cos, 0.f, -sin, 0.f, 1.f, 0.f, sin, 0.f, cos };
		mX.set(values2);

		cos = (float) Math.cos(azimuth);
		sin = (float) Math.sin(azimuth);
		float[] values3 = { cos, sin, 0.f, -sin, cos, 0.f, 0.f, 0.f, 1.f };
		mZ.set(values3);

		Matrix3 mat = mZ.mul(mY.mul(mX));

		Vector3 newFront = new Vector3(0.f, 1.f, 0.f).mul(mat);

		Vector3 z = new Vector3(0.f, 0.f, 1.f);

		return (float) Math.acos(z.dot(new Vector3(newFront.x, newFront.y, newFront.z)) / (float) Math.sqrt(newFront.x * newFront.x + newFront.y * newFront.y + newFront.z * newFront.z)) * 180.f / (float) Math.PI;
	}

	private List<Float> lowPassFilter(List<Float> input, List<Float> output, float alpha) {
		float result = 0.0f;

		/*
		 * if(output.size() <= 2){ return input; }
		 */

		if (output.size() < bufferSize) {
			output.add(0.0f);
			output.set(output.size() - 1, input.get(output.size() - 1));
		}

		for (int i = 1; i < output.size(); i++) {
			result = output.get(i) + alpha * (input.get(i) - output.get(i));
			output.set(i, result);
		}

		if (output.size() > bufferSize) {
			output.remove(0);
		}

		return output;
	}

	private float average(List<Float> input) {
		float result = 0.0f;

		for (int i = 0; i < input.size(); i++) {
			result += input.get(i);
		}

		return result / (float) input.size();
	}

	@Override
	public boolean keyDown(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	private float centerX = screenWidth * 1/6;
	private float centerY = screenHeight * 5/6;
	private float radius = screenWidth * 0.075f;

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// using the touchscreen to rotate camera
		
		if (button == Buttons.LEFT && !useSensorData) {
			
			float xDif = screenX - centerX;
			float yDif = screenY - centerY;
			float length = (float) Math.sqrt(xDif * xDif + yDif * yDif);
			
			if (length <= radius) {
				setAzimuthDir(-xDif / screenWidth / 0.075f);
				setRollDir(-yDif / screenHeight / 0.075f);
			}
			
			currentEvent = pointer;
		} else {
			setAzimuthDir(0);
			setRollDir(0);
		}

		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// set camera rotation to 0 when finger is lifted from touchscreen
		if (button == Buttons.LEFT) {
			rollDir = 0;
			azimuthDir = 0;
		}
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// changing camera rotation when finger is dragged on the touchscreen
		if (pointer == currentEvent) {
			
			float xDif = screenX - centerX;
			float yDif = screenY - centerY;
			float length = (float) Math.sqrt(xDif * xDif + yDif * yDif);
			
			if (length <= radius) {
				setAzimuthDir(-xDif / screenWidth / 0.075f);
				setRollDir(-yDif / screenHeight / 0.075f);
			} else {
				setAzimuthDir(0);
				setRollDir(0);
			}
			
		}
		return false;
	}

	private float limitSpeed(float wantedSpeed, float speedLimit) {
		if (wantedSpeed > 0) {
			return Math.min(wantedSpeed, speedLimit);
		}
		return Math.max(wantedSpeed, -speedLimit);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
}