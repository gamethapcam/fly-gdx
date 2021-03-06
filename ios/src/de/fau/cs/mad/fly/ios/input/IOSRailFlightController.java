package de.fau.cs.mad.fly.ios.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.fly.features.ICollisionListener;
import de.fau.cs.mad.fly.features.game.EndlessLevelGenerator;
import de.fau.cs.mad.fly.features.game.EndlessRailLevelGenerator;
import de.fau.cs.mad.fly.features.upgrades.types.Collectible;
import de.fau.cs.mad.fly.game.GameObject;
import de.fau.cs.mad.fly.player.Player;
import de.fau.cs.mad.fly.profile.PlayerProfile;
import de.fau.cs.mad.fly.res.GateDisplay;
import de.fau.cs.mad.fly.res.GateGoal;
import de.fau.cs.mad.fly.res.Perspective;

/**
 * Created by tschaei on 13.10.14.
 */
public class IOSRailFlightController extends IOSFlightController implements ICollisionListener {

    private EndlessRailLevelGenerator generator;

    private Vector3 direction;
    private Vector3 endPosition;
    private Vector3 currentPosition;

    private int listSize = 40;

    /** The center rail*/
    private List<Vector3> centerRail;
    /** Offset by which the other rails are shifted from the center rail*/
    private float railOffset = 3.f;
    /** Indicates the current Rail*/
    private int railX, railY;

    // Variables that control the changing of the current rail to an other one
    private boolean changeRailX, changeRailY = false;
    private float changeTimeX, changeTimeY = 0;
    private float changeX, changeY;

    /** Speed of the current plane*/
    private float planeSpeed, rollSpeed, azimuthSpeed;

    public IOSRailFlightController(Player player, PlayerProfile playerProfile, EndlessLevelGenerator generator, Perspective perspective) {
        super(player, playerProfile);
        this.generator = (EndlessRailLevelGenerator) generator;

        this.direction = perspective.viewDirection;
        this.currentPosition = perspective.position.cpy();
        this.endPosition = this.currentPosition.cpy();

        centerRail = new ArrayList<Vector3>();
        centerRail.add(currentPosition);

        this.railX = this.railY = 0;
    }

    @Override
    public void update(float delta) {
        if (useSensorData) {
            super.interpretSensorInput();
        }

        if(endPosition.equals(currentPosition)) {
            initRail();
        }

        changeRail(delta);

        if(checkRailPointPassed()) {
            Vector3 nextStep = nextStep();
            centerRail.add(nextStep);
            generator.addRailPosition(nextStep);

            // remove objects behind the passed point
            generator.removeComponents(centerRail.get(0));

            centerRail.remove(0);
            currentPosition = centerRail.get(0);

        }
        // increase the speed of the plane
        planeSpeed = player.getPlane().getCurrentSpeed() + 0.0001f;
        player.getPlane().setCurrentSpeed(planeSpeed);
    }

    private void initRail() {
        generator.setRail(centerRail);
        generator.setRailOffset(railOffset);

        this.currentPosition.add(direction.cpy().scl(5));
        this.endPosition = this.currentPosition.cpy();

        for(int i = 1; i < listSize; i++) {
            Vector3 nextStep = nextStep();
            centerRail.add(nextStep);
            generator.addRailPosition(nextStep);
        }
        generator.endInit();
    }

    private Vector3 nextStep() {
        Vector3 nextPos = new Vector3();

        nextPos = endPosition.cpy().add(direction);

        endPosition = nextPos;
        return nextPos;
    }

    private void changeRail(float delta) {
        rollSpeed = player.getPlane().getRollingSpeed();
        azimuthSpeed = player.getPlane().getAzimuthSpeed();

        Vector3 shiftVector = new Vector3();
        if(Math.abs(getRollFactor()) > 0.5) {
            if(Math.abs(railX + Math.signum(rollFactor)) <= 1.f && !changeRailX) {
                railX += Math.signum(rollFactor);
                changeX = Math.signum(rollFactor);
                changeTimeX = 1f;
            }
        }
        if(Math.abs(getAzimuthFactor()) > 0.5) {
            if(Math.abs(railY - Math.signum(azimuthFactor)) <= 1.f && !changeRailY) {
                railY -= Math.signum(azimuthFactor);
                changeY = -Math.signum(azimuthFactor);
                changeTimeY = 1f;
            }
        }

        if(changeTimeX == 1f) {
            changeRailX = true;
        }

        if(changeTimeY == 1f) {
            changeRailY = true;
        }
        Gdx.app.log("rails", "" + planeSpeed);
        if(changeRailX) {
            if(changeTimeX - delta * rollSpeed <= 0) {
                shiftVector.z = changeX * changeTimeX * railOffset * rollSpeed;
                changeRailX = false;
                changeX = 0;
            } else {
                changeTimeX -= delta * rollSpeed;
                shiftVector.z = changeX * delta * railOffset * rollSpeed;
            }

        }

        if(changeRailY) {
            if(changeTimeY - delta * azimuthSpeed <= 0) {
                shiftVector.x = changeY * changeTimeY * railOffset * azimuthSpeed;
                changeRailY = false;
                changeY = 0;
            } else {
                changeTimeY -= delta * azimuthSpeed;
                shiftVector.x = changeY * delta * railOffset * azimuthSpeed;
            }

        }

        player.getPlane().shift(shiftVector);
    }

    private boolean checkRailPointPassed() {
        if(player.getPlane().getPosition().y > centerRail.get(0).y + 3) {
            return true;
        }

        return false;
    }

    @Override
    public void onCollision(GameObject g1, GameObject g2) {

        if(g2 instanceof GateDisplay) {
            if(generator.checkAsteroidPosition(currentPosition, railX * railOffset, railY * railOffset)) {
                railX -= changeX;
                railY -= changeY;
            }

            player.getPlane().resetOnRail(railX * railOffset, railY * railOffset, currentPosition.y);

            changeX = changeY = 0;
            changeRailX = changeRailY = false;

        } else if(!(g2 instanceof GateGoal) && !(g2 instanceof Collectible)) {
            //asteroid
            while(generator.checkAsteroidPosition(currentPosition, railX * railOffset, railY * railOffset)) {
                Vector3 newPos = generateRandomAdjacentPosition();
            }

            player.getPlane().resetOnRail(railX * railOffset, railY * railOffset, currentPosition.y);

            changeX = changeY = 0;
            changeRailX = changeRailY = false;
        }
    }

    private Vector3 generateRandomAdjacentPosition() {
        int newRailX;
        int newRailY;

        if(Math.abs(railX) > 0) {
            newRailX = 0;
        } else {
            newRailX = (int) Math.signum(MathUtils.random(-1, 1));
        }

        if(Math.abs(railY) > 0) {
            newRailY = 0;
        } else {
            newRailY = (int) Math.signum(MathUtils.random(-1, 1));
        }

        railX = newRailX;
        railY = newRailY;

        return new Vector3(newRailX, newRailY, 0);
    }

}
