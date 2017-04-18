package com.example.rohan.teletouchandroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_DISTANCE = 50;

    private ImageView mHandImage;
    private SeekBar mPressureBar;
    private TextView mActuatorTextView;

    private static final Map<Position, Integer> mActuatorMap;

    static {
        Map<Position, Integer> actuatorMap = new HashMap<Position, Integer>();
        // Hard-coded position constants from a bunch of testing. Specific to this hand image.
        actuatorMap.put(new Position(160, 900), 1);
        actuatorMap.put(new Position(260, 960), 2);
        actuatorMap.put(new Position(360, 580), 3);
        actuatorMap.put(new Position(400, 700), 4);
        actuatorMap.put(new Position(410, 830), 5);
        actuatorMap.put(new Position(600, 560), 6);
        actuatorMap.put(new Position(600, 730), 7);
        actuatorMap.put(new Position(560, 815), 8);
        actuatorMap.put(new Position(830, 670), 9);
        actuatorMap.put(new Position(750, 780), 10);
        actuatorMap.put(new Position(690, 840), 11);
        actuatorMap.put(new Position(920, 880), 12);
        actuatorMap.put(new Position(840, 950), 13);
        actuatorMap.put(new Position(770, 975), 14);
        actuatorMap.put(new Position(470, 970), 15);
        actuatorMap.put(new Position(440, 1160), 16);
        actuatorMap.put(new Position(550, 950), 17);
        actuatorMap.put(new Position(500, 1160), 18);
        actuatorMap.put(new Position(680, 1060), 19);
        actuatorMap.put(new Position(620, 1190), 20);
        mActuatorMap = Collections.unmodifiableMap(actuatorMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandImage = (ImageView) findViewById(R.id.hand_image);
        mPressureBar = (SeekBar) findViewById(R.id.pressure_bar);
        mActuatorTextView = (TextView) findViewById(R.id.actuator_id_text);
    }

    private static double distance(int x0, int y0, int x1, int y1) {
        return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));
    }

    private int getActuatorIdFromPosition(int x, int y) {
        // Find the first distance that is less than the threshold
        for (Position p : mActuatorMap.keySet()) {
            int px = p.getX();
            int py = p.getY();

            if (distance(x, y, px, py) < MAX_DISTANCE) {
                return mActuatorMap.get(p);
            }
        }

        // Not found
        return -1;
    }

    private void updateActuatorText(int actuatorId) {
        String text = "";

        if (actuatorId == -1) {
            text = "No data being sent...";
        } else {
            text ="Sending data to actuator #" + actuatorId;
        }

        mActuatorTextView.setText(text);
    }

    private void sendPressureData(int x, int y) {
        int actuatorId = getActuatorIdFromPosition(x, y);
        updateActuatorText(actuatorId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                sendPressureData(x, y);
            default:
                break;
        }

        return true;
    }

    private static class Position {

        private int x;
        private int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

    }

}
