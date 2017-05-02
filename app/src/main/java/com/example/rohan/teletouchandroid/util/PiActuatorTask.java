package com.example.rohan.teletouchandroid.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by rohan on 4/18/17.
 */

public class PiActuatorTask extends AsyncTask<Integer, Void, Void> {

    private String hostAddress;
    private int port;

    public PiActuatorTask(String hostAddress, int port) {
        this.hostAddress = hostAddress;
        this.port = port;
    }

    /**
     * Builds a string representing a Python dictionary mapping locations on the hand to pressure
     * intensities at those locations.
     *
     * @param handId    handId that is currently being touched
     * @param intensity intensity at that position
     * @return String of Python-interpretable dictionary with hand to pressure mapping.
     */
    public String buildPressureDict(int handId, int intensity) {
        String result = "{ ";
        int startingId = 1;
        int endingId = 20;

        // Build dictionary with all intensities
        for (int id = startingId; id <= endingId; id++) {
            int handIntensity = (handId == id) ? intensity : 0;
            result += id + ": " + handIntensity;
            result += (id < endingId) ? ", " : " }";
        }

        return result;
    }

    @Override
    protected Void doInBackground(Integer... params) {
        int id = params[0];
        int intensity = params[1];
        String pressureDict = buildPressureDict(id, intensity);

        try {
            Socket socket = new Socket(hostAddress, port);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(pressureDict);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
