package de.fau.cs.mad.fly.res;

import java.util.ArrayList;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Matrix4;

import de.fau.cs.mad.fly.Assets;
import de.fau.cs.mad.fly.game.GameObject;

public class Gate {

	public static final int NO_GATE = -1;

	public int id;

	public String modelId;

	public float[] transformMatrix;
	
	public ArrayList<Integer> successors = new ArrayList<Integer>();
	
	public GameObject model = null;
	
	private ModelBatch batch;
	//private PerspectiveCamera camera;
	private Environment environment;
	
	private boolean visible = true;
	
	public Gate() {
		// DUMMY
	}
	
	public Gate(AssetDescriptor<Model> modelAsset, Matrix4 transformMatrix, ModelBatch batch, Environment environment) {
		this.batch = batch;
		//this.camera = camera;
		this.environment = environment;

		model = new GameObject(Assets.manager.get(modelAsset));
		model.transform = transformMatrix;
	}
	
	// TODO: use batch and environment from constructor
	public void render(ModelBatch batch, PerspectiveCamera camera, Environment env) {
		if(visible && model.isVisible(camera)) {
			batch.render(model, env);
		}
	}
	
	public void setColor(Color color) {
		model.materials.get(0).set(ColorAttribute.createDiffuse(color));
	}
	
	public void setVisibility(boolean visible) {
		this.visible = visible;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || obj != null && obj instanceof Gate && id == ((Gate) obj).id;
	}
}
