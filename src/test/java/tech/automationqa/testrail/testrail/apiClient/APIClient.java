package tech.automationqa.testrail.testrail.apiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * APIClient handles communication with the TestRail API by sending HTTP requests.
 * This class provides methods to perform GET and POST requests to the specified TestRail instance.
 */
public class APIClient {
    private String user;
    private String password;
    private final String url;

    /**
     * Constructor for APIClient.
     *
     * @param url The base URL of the TestRail API.
     */
    public APIClient(String url) {
        this.url = url;
    }

    /**
     * Sets the user for API authentication.
     *
     * @param user The username for the TestRail API.
     * @return The current instance of APIClient.
     */
    public APIClient setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * Sets the password for API authentication.
     *
     * @param password The password for the TestRail API.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sends a GET request to the specified URI of the TestRail API.
     *
     * @param uri The API endpoint URI.
     * @return An object containing the parsed JSON response.
     */
    public Object sendGet(String uri) {
        try {
            return sendRequest("GET", uri, null);
        } catch (Exception e) {
            throw new APIException("Failed to send GET request due to network issues.", e);
        }
    }

    /**
     * Sends a POST request to the specified URI of the TestRail API.
     *
     * @param uri  The API endpoint URI.
     * @param data The JSON string to be sent as the request body.
     * @return An object containing the parsed JSON response.
     */
    public Object sendPost(String uri, String data) {
        try {
            return sendRequest("POST", uri, data);
        } catch (Exception e) {
            throw new APIException("Failed to send POST request due to network issues.", e);
        }
    }

    private HttpURLConnection createConnection(String method, String uri) throws IOException {
        URL endpoint;
        try {
            endpoint = new URI(this.url + uri).toURL();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod(method);
        conn.addRequestProperty("Authorization", "Basic " + getAuthorization());
        conn.addRequestProperty("Content-Type", "application/json");
        return conn;
    }

    private Object sendRequest(String method, String uri, String data) throws IOException, APIException {
        HttpURLConnection conn = createConnection(method, uri);

        if ("POST".equals(method) && data != null) {
            conn.setDoOutput(true);
            try (OutputStream outputStream = conn.getOutputStream()) {
                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
            }
        }

        int status = conn.getResponseCode();
        try (InputStream istream = (status != 200) ? conn.getErrorStream() : conn.getInputStream()) {
            if (istream == null) {
                throw new APIException("TestRail API returned HTTP " + status + " without a response body.");
            }

            if (uri.startsWith("get_attachment/") && status == 200) {
                return saveAttachment(istream, data);
            }

            String responseBody = readStream(istream);
            return parseResponse(status, responseBody);
        }
    }

    private String saveAttachment(InputStream istream, String filePath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = istream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return filePath;
    }

    private String readStream(InputStream istream) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(istream, StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }

    private Object parseResponse(int status, String responseBody) throws APIException {
        if (responseBody.isEmpty()) {
            return new JSONObject();
        }

        Object result = responseBody.startsWith("[") ? new JSONArray(responseBody) : new JSONObject(responseBody);
        if (status != 200) {
            String error = extractErrorMessage(result);
            throw new APIException("TestRail API returned HTTP " + status + " (" + error + ")");
        }

        return result;
    }

    private String extractErrorMessage(Object response) {
        if (response instanceof JSONObject) {
            JSONObject obj = (JSONObject) response;
            if (obj.has("error")) {
                return '"' + obj.getString("error") + '"';
            }
        }
        return "No additional error message received";
    }

    private String getAuthorization() {
        return Base64.getEncoder().encodeToString((this.user + ":" + this.password).getBytes(StandardCharsets.UTF_8));
    }
}
