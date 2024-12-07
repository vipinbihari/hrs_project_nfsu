package com.hrs.network;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class RawHttpClient {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_RESPONSE_WAIT = 10000; // 10 seconds

    private final SSLSocketFactory sslSocketFactory;

    public RawHttpClient() {
        this.sslSocketFactory = createSSLSocketFactory();
    }

    private SSLSocketFactory createSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            NetworkLogger.error("Failed to create SSL socket factory", e);
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    public String sendRequest(String host, int port, String path, boolean isHttps, String rawRequest) throws IOException {
        NetworkLogger.log("Parsed URL - Host: " + host + ", Port: " + port + ", Path: " + path + ", HTTPS: " + isHttps);
        NetworkLogger.log("Connecting to " + host + ":" + port);
        
        try (Socket socket = createSocket(host, port)) {
            // Use the provided raw request
            NetworkLogger.log("Sending request:\n" + rawRequest);
            
            // Convert request to bytes and log hex
            byte[] requestBytes = rawRequest.getBytes(StandardCharsets.UTF_8);
            StringBuilder hexLog = new StringBuilder("Request bytes in hex:\n");
            for (int i = 0; i < requestBytes.length; i++) {
                String hex = String.format("%02x", requestBytes[i]);
                hexLog.append(hex).append(" ");
                if (requestBytes[i] == '\r') hexLog.append("[CR]");
                if (requestBytes[i] == '\n') hexLog.append("[LF]");
                if ((i + 1) % 16 == 0) hexLog.append("\n");
            }
            NetworkLogger.log(hexLog.toString());
            
            // Get streams
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Write request
            NetworkLogger.log("Writing " + requestBytes.length + " bytes to socket");
            out.write(requestBytes);
            out.flush();
            NetworkLogger.log("Request written and flushed to socket");
            
            // Small delay to ensure request is sent
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Read response
            String response = readResponse(in);
            
            // Don't close the socket here - let try-with-resources handle it
            return response;
        }
    }

    public CompletableFuture<RawHttpResponse> sendRawRequest(String host, int port, String rawRequest) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Only normalize existing line endings to CRLF, no automatic additions
                String normalizedRequest = rawRequest.replaceAll("\\r\\n|\\n", "\r\n");

                // Parse the first line to get method and path
                String[] lines = normalizedRequest.split("\r\n", 2);
                String firstLine = lines[0];
                String path = "/";
                if (firstLine.contains(" ")) {
                    String[] parts = firstLine.split(" ");
                    if (parts.length > 1) {
                        path = parts[1];
                    }
                }

                // Send the request using the normalized raw request
                String response = sendRequest(host, port, path, port == 443, normalizedRequest);
                
                long endTime = System.currentTimeMillis();
                NetworkLogger.log(String.format("Response received in %d ms", endTime - startTime));
                NetworkLogger.log("Response:\n" + response);
                
                return new RawHttpResponse(response, endTime - startTime, null);
            } catch (Exception e) {
                NetworkLogger.log("Failed to send/receive request: " + e.getMessage());
                long endTime = System.currentTimeMillis();
                return new RawHttpResponse(null, endTime - startTime, e.getMessage());
            }
        });
    }

    private String readResponse(InputStream in) throws IOException {
        StringBuilder response = new StringBuilder();
        byte[] buffer = new byte[32768];
        int bytesRead;
        int totalBytesRead = 0;
        int readAttempts = 0;
        final int maxAttempts = 10; // Increased max attempts
        
        NetworkLogger.log("Starting to read response...");
        
        try {
            while (readAttempts < maxAttempts) {
                readAttempts++;
                
                try {
                    // Try to read available data
                    int available = in.available();
                    if (available > 0) {
                        NetworkLogger.log("Data available: " + available + " bytes");
                        bytesRead = in.read(buffer, 0, Math.min(available, buffer.length));
                    } else {
                        // If no data is available, do a blocking read with timeout
                        NetworkLogger.log("No data available, attempting blocking read (attempt " + readAttempts + ")");
                        bytesRead = in.read(buffer);
                    }
                    
                    if (bytesRead == -1) {
                        NetworkLogger.log("End of stream reached on attempt " + readAttempts);
                        if (totalBytesRead > 0) {
                            // If we have data, consider the response complete
                            break;
                        }
                        // No data yet, wait longer between attempts
                        Thread.sleep(200);
                        continue;
                    }
                    
                    totalBytesRead += bytesRead;
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    response.append(chunk);
                    
                    // Log received data
                    StringBuilder hexLog = new StringBuilder("Received bytes in hex:\n");
                    for (int i = 0; i < bytesRead; i++) {
                        String hex = String.format("%02x", buffer[i]);
                        hexLog.append(hex).append(" ");
                        if (buffer[i] == '\r') hexLog.append("[CR]");
                        if (buffer[i] == '\n') hexLog.append("[LF]");
                        if ((i + 1) % 16 == 0) hexLog.append("\n");
                    }
                    NetworkLogger.log(hexLog.toString());
                    
                    // If we have a complete response with headers and some body, we can stop
                    if (response.indexOf("\r\n\r\n") != -1) {
                        String headers = response.substring(0, response.indexOf("\r\n\r\n"));
                        NetworkLogger.log("Received headers:\n" + headers);
                        
                        // Check if we have some body data after headers
                        if (response.length() > headers.length() + 4) {
                            NetworkLogger.log("Got complete response with body");
                            break;
                        }
                        
                        // If headers indicate no body, we can stop
                        String lowerHeaders = headers.toLowerCase();
                        if (lowerHeaders.contains("content-length: 0") || 
                            (lowerHeaders.contains("transfer-encoding: chunked") && response.toString().endsWith("0\r\n\r\n"))) {
                            NetworkLogger.log("Response complete based on headers");
                            break;
                        }
                    }
                    
                    // Wait a bit between reads if we haven't found the end yet
                    if (readAttempts < maxAttempts) {
                        Thread.sleep(100);
                    }
                    
                } catch (SocketTimeoutException e) {
                    NetworkLogger.log("Read timeout on attempt " + readAttempts);
                    if (response.length() > 0) break;
                    if (readAttempts >= maxAttempts) throw e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            NetworkLogger.log("Finished reading response. Total bytes read: " + totalBytesRead + " in " + readAttempts + " attempts");
            
            if (totalBytesRead == 0) {
                NetworkLogger.log("Warning: No bytes were read from the response after " + readAttempts + " attempts");
                return "No response received from server";
            }
            
            return response.toString();
            
        } catch (Exception e) {
            NetworkLogger.log("Exception while reading response: " + e.getMessage());
            if (totalBytesRead > 0) {
                return response.toString();
            }
            throw new IOException("Failed to read response: " + e.getMessage(), e);
        }
    }

    private void quietlyClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                NetworkLogger.error("Error closing resource", e);
            }
        }
    }

    private Socket createSocket(String host, int port) throws IOException {
        Socket socket;
        if (port == 443) {
            NetworkLogger.log("Creating SSL socket for HTTPS connection");
            try {
                // Create socket and connect first
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(10000);
                socket.setKeepAlive(true);
                socket.setReceiveBufferSize(32768); // Increase receive buffer
                socket.setSendBufferSize(32768);    // Increase send buffer
                
                NetworkLogger.log("Connecting socket to " + host + ":" + port);
                socket.connect(new InetSocketAddress(host, port), 10000);
                
                // Create SSLContext with TLSv1.2
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
                }, new SecureRandom());
                
                // Create SSLSocket with SNI extension
                SSLSocketFactory factory = sslContext.getSocketFactory();
                SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, host, port, true);
                
                // Enable SNI
                SNIHostName sniHostname = new SNIHostName(host);
                List<SNIServerName> sniNames = new ArrayList<>();
                sniNames.add(sniHostname);
                SSLParameters params = sslSocket.getSSLParameters();
                params.setServerNames(sniNames);
                sslSocket.setSSLParameters(params);
                
                // Set TLSv1.2 only
                sslSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
                
                // Log protocols and start handshake
                NetworkLogger.log("Available protocols: " + String.join(", ", sslSocket.getSupportedProtocols()));
                NetworkLogger.log("Starting SSL handshake with SNI: " + host);
                sslSocket.startHandshake();
                NetworkLogger.log("SSL handshake completed");
                
                // Log the negotiated protocol and cipher suite
                NetworkLogger.log("Negotiated protocol: " + sslSocket.getSession().getProtocol());
                NetworkLogger.log("Negotiated cipher suite: " + sslSocket.getSession().getCipherSuite());
                
                // Small delay after handshake
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                socket = sslSocket;
                
            } catch (Exception e) {
                throw new IOException("Failed to create SSL socket: " + e.getMessage(), e);
            }
        } else {
            NetworkLogger.log("Creating plain socket for HTTP connection");
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(10000);
            socket.setKeepAlive(true);
            
            NetworkLogger.log("Connecting socket to " + host + ":" + port);
            socket.connect(new InetSocketAddress(host, port), 10000);
        }
        
        return socket;
    }

    public static class RawHttpResponse {
        private final String rawResponse;
        private final long responseTimeMs;
        private final String error;

        public RawHttpResponse(String rawResponse, long responseTimeMs, String error) {
            this.rawResponse = rawResponse;
            this.responseTimeMs = responseTimeMs;
            this.error = error;
        }

        public String getRawResponse() { return rawResponse; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getError() { return error; }
        public boolean hasError() { return error != null && !error.isEmpty(); }
    }

    public static class UrlParser {
        private final String host;
        private final int port;
        private final String path;
        private final boolean isHttps;

        public UrlParser(String url) throws MalformedURLException {
            URL parsedUrl = new URL(url);
            this.host = parsedUrl.getHost();
            this.path = parsedUrl.getPath().isEmpty() ? "/" : parsedUrl.getPath();
            this.isHttps = parsedUrl.getProtocol().equalsIgnoreCase("https");
            this.port = parsedUrl.getPort() != -1 ? parsedUrl.getPort() : 
                       (isHttps ? 443 : 80);
            
            NetworkLogger.log(String.format("Parsed URL - Host: %s, Port: %d, Path: %s, HTTPS: %b",
                                          host, port, path, isHttps));
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getPath() { return path; }
        public boolean isHttps() { return isHttps; }
    }
}
