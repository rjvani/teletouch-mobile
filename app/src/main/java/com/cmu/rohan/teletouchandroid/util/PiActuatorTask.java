package com.cmu.rohan.teletouchandroid.util;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by rohan on 4/18/17.
 */
public class PiActuatorTask extends AsyncTask<String, Void, Void> {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String hostAddress;
    private String apiUrl;
    private int port;
    private List<String> pressureDictionaries;

    public PiActuatorTask(String hostAddress, int port) {
        this.hostAddress = hostAddress;
        this.port = port;
    }

    public PiActuatorTask(String hostAddress, int port, int actuatorId, int intensity) {
        this.hostAddress = hostAddress;
        this.port = port;
        this.pressureDictionaries = new ArrayList<String>();

        pressureDictionaries.add(buildPressureDict(actuatorId, intensity));
    }

    public PiActuatorTask(String hostAddress, int port, List<String> recordedList, String apiUrl) {
        this.hostAddress = hostAddress;
        this.apiUrl = apiUrl;
        this.port = port;
        this.pressureDictionaries = recordedList;
    }

    /**
     * Builds a string representing a Python dictionary mapping locations on the hand to pressure
     * intensities at those locations.
     *
     * @param handId    handId that is currently being touched
     * @param intensity intensity at that position
     * @return String of Python-interpretable dictionary with hand to pressure mapping.
     */
    public static String buildPressureDict(int handId, int intensity) {
        return "{\"" + handId + "\":" + intensity + "}";
    }

    private String convertPressureDictionary() {
        String result = "[";

        // Format each dictionary and concatenate to result
        for (int index = 0; index < pressureDictionaries.size(); index++) {
            String dictS = pressureDictionaries.get(index);
            result += dictS;
            result += (index < pressureDictionaries.size() - 1) ? ", " : "";
        }

        return result + "]";
    }

    private void sendRecordingIdToPi(String recordingId) {
        try {
            String recordingData = "{ \"recordingId\": \"" + recordingId + "\" }";
            Socket socket = new Socket(hostAddress, port);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(recordingData);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postRecording(final String recordingId) {
        OkHttpClient client = new OkHttpClient();
        String body = "{ \"" + recordingId + "\": " + convertPressureDictionary() + "}";
        RequestBody requestBody = RequestBody.create(JSON, body);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();

        client
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        sendRecordingIdToPi(recordingId);
                    }

                });
    }

    private void saveRecording() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        client
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String jsonData = response.body().string();
                        try {
                            JSONArray jsonArray = new JSONArray(jsonData);
                            String recordingId = "recording" + jsonArray.length();
                            postRecording(recordingId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                });
    }

    @Override
    protected Void doInBackground(String... params) {
        // See if a recording ID needs to be sent
        if (params.length > 0) {
            sendRecordingIdToPi(params[0]);
            return null;
        }

        // See if a recording needs to be saved on the server
        if (pressureDictionaries.size() > 1) {
            saveRecording();
        } else {
            String convertedDictionaries = convertPressureDictionary();

            try {
                Socket socket = new Socket(hostAddress, port);
                DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
                DOS.writeUTF(convertedDictionaries);
                socket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
