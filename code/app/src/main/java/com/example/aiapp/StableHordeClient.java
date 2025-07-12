package com.example.aiapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class StableHordeClient {

    public interface ImageCallback {
        void onSuccess(Bitmap image);
        void onError(String message);
    }

    public static void generateImage(String prompt, ImageCallback callback) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("prompt", prompt);

                JSONObject params = new JSONObject();
                params.put("sampler_name", "k_euler");
                //params.put("denoising_strength", 0.75);
                params.put("width", 512);
                params.put("height", 512);
                params.put("steps", 30);
                params.put("cfg_scale", 7.5);
                params.put("n", 1);
                // params.put("post_processing", new JSONArray().put("GFPGAN"));
                payload.put("params", params);

                payload.put("nsfw", true);
                payload.put("trusted_workers", false);
                payload.put("censor_nsfw", false);
                payload.put("models", new JSONArray().put("stable_diffusion"));
                payload.put("r2", true);

                URL url = new URL("https://stablehorde.net/api/v2/generate/async");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", "xxxxxxx);
                conn.setRequestProperty("Client-Agent", "xxxxxxx:0.1 (agentxd: xxxxx)");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes());
                }

                int statusCode = conn.getResponseCode();
                Log.d("StableHorde", "Submit status code: " + statusCode);

                InputStream responseStream;
                if (statusCode == 202) {
                    responseStream = conn.getInputStream();
                } else {
                    responseStream = conn.getErrorStream();
                    Scanner errScanner = new Scanner(responseStream).useDelimiter("\\A");
                    String errorResponse = errScanner.hasNext() ? errScanner.next() : "Unknown error";
                    throw new Exception("Submission failed: " + errorResponse);
                }

                Scanner s = new Scanner(responseStream).useDelimiter("\\A");
                String responseBody = s.hasNext() ? s.next() : "";
                JSONObject responseJson = new JSONObject(responseBody);
                String taskId = responseJson.getString("id");

                Log.d("StableHorde", "Task ID: " + taskId);
                String pollUrl = "https://stablehorde.net/api/v2/generate/status/" + taskId;

                while (true) {
                    Thread.sleep(10000);    // 10 secs

                    HttpURLConnection pollConn = (HttpURLConnection) new URL(pollUrl).openConnection();
                    pollConn.setRequestProperty("apikey", "xxxxxx");
                    pollConn.setRequestProperty("Client-Agent", "xxxx:0.1 (agentxd: xxxxxxx)");
                    pollConn.setRequestProperty("Content-Type", "application/json");

                    int pollStatus = pollConn.getResponseCode();
                    Log.d("StableHorde", "Poll status: " + pollStatus);

                    InputStream pollStream;
                    if (pollStatus == 200) {
                        pollStream = pollConn.getInputStream();
                    } else {
                        pollStream = pollConn.getErrorStream();
                        Scanner errScan = new Scanner(pollStream).useDelimiter("\\A");
                        String pollErr = errScan.hasNext() ? errScan.next() : "Poll error";
                        throw new Exception("Polling failed: " + pollErr);
                    }

                    Scanner ps = new Scanner(pollStream).useDelimiter("\\A");
                    String pollResult = ps.hasNext() ? ps.next() : "";
                    JSONObject pollJson = new JSONObject(pollResult);

                    if (pollJson.optBoolean("done", false)) {
                        JSONArray gens = pollJson.optJSONArray("generations");
                        if (gens != null && gens.length() > 0) {
                            String imgUrl = gens.getJSONObject(0).getString("img");
                            Log.d("StableHorde", "Image URL: " + imgUrl);
                            try {
                                HttpURLConnection imgConn = (HttpURLConnection) new URL(imgUrl).openConnection();
                                imgConn.setDoInput(true);
                                imgConn.connect();
                                InputStream imgStream = imgConn.getInputStream();
                                Bitmap bitmap = BitmapFactory.decodeStream(imgStream);

                                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(bitmap));
                            } catch (Exception imgEx) {
                                imgEx.printStackTrace();
                                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to download image"));
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("No image returned."));
                        }
                        return;
                    }

                    if (pollJson.optBoolean("faulted", false)) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Generation faulted."));
                        return;
                    }

                    Log.d("StableHorde", "Waiting...");
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }
}
