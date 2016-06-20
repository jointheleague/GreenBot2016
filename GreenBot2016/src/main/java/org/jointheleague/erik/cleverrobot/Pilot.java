package org.jointheleague.erik.cleverrobot;

import android.os.SystemClock;
import android.util.Log;

import org.jointheleague.erik.cleverrobot.sensors.UltraSonicSensors;
import org.jointheleague.erik.irobot.IRobotAdapter;
import org.jointheleague.erik.irobot.IRobotInterface;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class Pilot extends IRobotAdapter
{

    private static final String TAG = "Pilot";
    // The following measurements are taken from the interface specification
    private static final double WHEEL_DISTANCE = 235.0; //in mm
    private static final double WHEEL_DIAMETER = 72.0; //in mm
    private static final double ENCODER_COUNTS_PER_REVOLUTION = 508.8;
    private final Dashboard dashboard;
    public UltraSonicSensors sonar;
    int angle;
    private int startLeft;
    private int startRight;
    private int countsToGoWheelLeft;
    private int countsToGoWheelRight;
    private int directionLeft;
    private int directionRight;
    private static final int STRAIGHT_SPEED = 200;
    private static final int TURN_SPEED = 100;
    private final boolean debug = true; // Set to true to get debug messages.
    public boolean isStuckRoomba = false;

    public Pilot(IRobotInterface iRobot, Dashboard dashboard, IOIO ioio)
            throws ConnectionLostException
    {
        super(iRobot);
        sonar = new UltraSonicSensors(ioio);
        this.dashboard = dashboard;
        dashboard.log(dashboard.getString(R.string.hello));
    }

    /**
     * This method is executed when the robot first starts up.
     **/
    public void initialize() throws ConnectionLostException
    {
        safe();
        readSensors(SENSORS_GROUP_ID100); // Read battery info
        int batteryPerCent = (getBatteryCharge() * 100 / getBatteryCapacity());
        dashboard.log(getBatteryCharge() + "   " + getBatteryCapacity());
        if (batteryPerCent > 80)
        {
            dashboard.log("Good morning, sir...My battery is at " + batteryPerCent + "% and I am ready to roll.");
        } else if (batteryPerCent < 40)
        {
            dashboard.log("Good morning, sir.  I am sorry to report that my battery is less than 40% charged.  It is at " + batteryPerCent + "%.  You are in danger of running out of juice.  I recommend that you charge as soon as possible.");
        }
        driveDirect(100, 100);
    }

    /**
     * This method is called repeatedly.
     **/
    public void loop() throws ConnectionLostException
    {
        dashboard.log("loop");
        SystemClock.sleep(1000);
        readSensors(SENSORS_GROUP_ID100);

        if (isLightBump())
        {
            dashboard.log("bump");
        }
        dashboard.log(getCurrent() + "");
        if (Math.abs(getCurrent()) > 1000)
        {
           isStuckRoomba = true;
        }

    }

    private void shutDown() throws ConnectionLostException
    {
        dashboard.log("Shutting down... Bye!");
        stop();
        closeConnection();
    }


    /**
     * Moves the robot in a straight line. Note: Unexpected behavior may occur if distance
     * is larger than 14567mm.
     *
     * @param distance the distance to go in mm. Must be &le; 14567.
     */
    private void goStraight(int distance) throws ConnectionLostException
    {
        countsToGoWheelLeft = (int) (distance * ENCODER_COUNTS_PER_REVOLUTION
                / (Math.PI * WHEEL_DIAMETER));
        countsToGoWheelRight = countsToGoWheelLeft;
        if (debug)
        {
            String msg = String.format("Going straight  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = 1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * STRAIGHT_SPEED, directionRight * STRAIGHT_SPEED);
    }


    /**
     * Turns in place rightwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnRight(int degrees) throws ConnectionLostException
    {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        directionLeft = 1;
        directionRight = -1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
        if (debug)
        {
            String msg = String.format("Turning right  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
    }

    /**
     * Turns in place leftwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnLeft(int degrees) throws ConnectionLostException
    {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        if (debug)
        {
            String msg = String.format("Turning left  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = -1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
    }

    private void recordEncodersAndDrive(int leftVelocity, int rightVelocity) throws ConnectionLostException
    {
        readSensors(SENSORS_GROUP_ID101);
        startLeft = getEncoderCountLeft();
        startRight = getEncoderCountRight();
        driveDirect(leftVelocity, rightVelocity);
    }
}
