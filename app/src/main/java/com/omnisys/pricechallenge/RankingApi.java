
package com.omnisys.pricechallenge;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RankingApi {

    private static final String BASE_URL =
            "http://192.168.80.118:5000";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private RankingApi() {
        // Ez da klase honen objekturik sortu behar.
    }

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    private static void request(
            String path,
            String method,
            JSONObject body,
            Callback callback) {

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(BASE_URL + path);
                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);

                //_______________________
                connection.setUseCaches(false);
                connection.setRequestProperty("Connection", "close");
                //_______________________
                connection.setRequestProperty(
                        "Accept", "application/json");

                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                            "Content-Type", "application/json; charset=UTF-8");

                    try (OutputStream output =
                                 connection.getOutputStream()) {
                        output.write(body.toString().getBytes(
                                StandardCharsets.UTF_8));
                    }
                }

                int status = connection.getResponseCode();

                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder result = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 stream,
                                                 StandardCharsets.UTF_8))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                    }
                }

                if (status >= 200 && status < 300) {
                    JSONObject json = new JSONObject(result.toString());

                    mainHandler.post(
                            () -> callback.onSuccess(json));
                } else {
                    String error = "HTTP " + status + ": " + result;

                    mainHandler.post(
                            () -> callback.onError(error));
                }

            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "Ezin izan da zerbitzariarekin konektatu."
                        : e.getMessage();

                mainHandler.post(
                        () -> callback.onError(message));

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Zerbitzaria martxan dagoen egiaztatu.
    public static void checkServer(Callback callback) {
        request("/api/health", "GET", null, callback);
    }

    // Partida baten emaitza zerbitzarian gorde.
    public static void saveResult(
            String resultId,
            String username,
            int score,
            int correctAnswers,
            int totalQuestions,
            long playedAt,
            Callback callback) {

        try {
            JSONObject body = new JSONObject();

            body.put("resultId", resultId);
            body.put("username", username);
            body.put("score", score);
            body.put("correctAnswers", correctAnswers);
            body.put("totalQuestions", totalQuestions);
            body.put("playedAt", playedAt);

            request("/api/results", "POST", body, callback);

        } catch (Exception e) {
            mainHandler.post(() ->
                    callback.onError(
                            "Ezin izan da emaitza prestatu."));
        }
    }

    // TOP 5 partekatua irakurri.
    public static void getTopFive(Callback callback) {
        request("/api/leaderboard?limit=5",
                "GET", null, callback);
    }
}
