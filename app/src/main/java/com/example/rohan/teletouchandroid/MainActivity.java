package com.example.rohan.teletouchandroid;

import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_DISTANCE = 50;

    private String mHostAddress;
    private int mPort;

    private Button mSettingsMenuButton;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                showNetworkDialog();
                break;
            default:
                break;
        }

        return true;
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

    private void showNetworkDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(50, 50, 50, 0);

        final TextView ipTitle = new TextView(this);
        ipTitle.setText("Host Address");

        final TextView portTitle = new TextView(this);
        portTitle.setText("Port");

        final EditText ipInput = new EditText(this);
        ipInput.setTextSize(14);
        ipInput.setHint(R.string.ip_hint_text);

        final EditText portInput = new EditText(this);
        portInput.setTextSize(14);
        portInput.setHint(R.string.port_hint_text);

        layout.addView(ipTitle);
        layout.addView(ipInput);
        layout.addView(portTitle);
        layout.addView(portInput);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("Network Configuration")
                .setMessage("Set up your server's IP Address and port for communication.")
                .setView(layout)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mHostAddress = ipInput.getText().toString();
                        mPort = Integer.parseInt(portInput.getText().toString());
                    }

                })
                .setNegativeButton("Cancel", null);

        Dialog dialog = alertDialogBuilder.create();
        dialog.show();
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
