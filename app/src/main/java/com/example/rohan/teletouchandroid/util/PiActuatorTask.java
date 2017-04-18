package com.example.rohan.teletouchandroid.util;

import android.os.AsyncTask;

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

    @Override
    protected Void doInBackground(Integer... params) {
        int id = params[0];
        int intensity = params[1];

        try {
            Socket socket = new Socket(hostAddress, port);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(id + "," + intensity);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
