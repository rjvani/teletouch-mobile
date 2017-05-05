package com.example.rohan.teletouchandroid;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rohan.teletouchandroid.util.PiActuatorTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final String API_ADDRESS = "http://teletouch.herokuapp.com/api/address";
    private static final String API_RECORDINGS = "http://teletouch.herokuapp.com/api/recordings";
    private static final int MAX_DISTANCE = 50;
    private static final int RECORDING_R = 0x7F;
    private static final int RECORDING_G = 0x40;
    private static final int RECORDING_B = 0x40;
    private static final Handler mHandler;
    private static final Map<Position, Integer> mActuatorMap;

    private final Runnable mRunnable;

    private Button mRecordingButton;
    private SeekBar mPressureBar;

    private List<String> mRecordedList;
    private String mHostAddress;
    private int mPort;
    private int mPressureIntensity;
    private int mRecordingIndex;
    private boolean mCurrentlyRecording;

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
        mHandler = new Handler();
    }

    public MainActivity() {
        super();

        mPort = 5005;
        mRecordedList = new ArrayList<String>();
        mRunnable = new Runnable() {

            @Override
            public void run() {
                updateRecordingBackground();
                mHandler.postDelayed(this, 50);
            }

        };
    }

    private void initNetwork() {
        final Context context = this;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(API_ADDRESS)
                .build();

        // Send request to server to load and save the IP address of the receiver PI
        client
            .newCall(request)
            .enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(context, "Cannot get IP Address!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        String strippedJson = jsonData.substring(1, jsonData.length() - 1);
                        JSONObject jsonObject = new JSONObject(strippedJson);
                        mHostAddress = jsonObject.getString("ip");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNetwork();

        mPressureBar = (SeekBar) findViewById(R.id.pressure_bar);
        mRecordingButton = (Button) findViewById(R.id.record_button);

        mPressureBar.setOnSeekBarChangeListener(this);
        mRecordingButton.setOnClickListener(this);

        mRecordingIndex = 0;
        mCurrentlyRecording = false;
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

    private void sendPressureData(int x, int y) {
        int actuatorId = getActuatorIdFromPosition(x, y);
        // Send data to pi
        if (actuatorId != -1) {
            mRecordedList.add(PiActuatorTask.buildPressureDict(actuatorId, mPressureIntensity));
            new PiActuatorTask(mHostAddress, mPort, actuatorId, mPressureIntensity).execute();
        }
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
        String defaultHost = getResources().getString(R.string.ip_hint_text);
        ipInput.setHint((mHostAddress != "") ? mHostAddress : defaultHost);

        final EditText portInput = new EditText(this);
        portInput.setTextSize(14);
        String defaultPort = getResources().getString(R.string.port_hint_text);
        portInput.setHint((mPort != 0) ? Integer.toString(mPort) : defaultPort);

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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.mPressureIntensity = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateRecordingBackground() {
        Double newR = RECORDING_R + RECORDING_R * Math.sin(mRecordingIndex);
        Double newG = RECORDING_G + RECORDING_G * Math.sin(mRecordingIndex);
        Double newB = RECORDING_B + RECORDING_B * Math.sin(mRecordingIndex);
        int newColor = (0xFF << 24) | (newR.intValue() << 16) | (newG.intValue() << 8) | newB.intValue();
        mRecordingButton.setBackgroundColor(newColor);
        mRecordingIndex++;
    }

    private void onClickRecording() {
        mCurrentlyRecording = !mCurrentlyRecording;

        if (mCurrentlyRecording) {
            mRecordedList = new ArrayList<String>();
            mHandler.post(mRunnable);
        } else {
            mRecordingButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            mHandler.removeCallbacks(mRunnable);
            // Determine if we need to send data to the pi
            if (mRecordedList.size() > 0) {
                new PiActuatorTask(mHostAddress, mPort, mRecordedList).execute();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_button:
                onClickRecording();
                break;
            default:
                break;
        }
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
