package com.transportlogistics.app.notification.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal loopback SMTP server used to exercise the real Jakarta Mail transport. */
public final class LocalSmtpTestServer implements Closeable {
    public enum Scenario { ACCEPT, TEMPORARY_RECIPIENT_REJECTION, PERMANENT_RECIPIENT_REJECTION, SENDER_REJECTION, AUTHENTICATION_REJECTION, GREETING_TIMEOUT }

    private final ServerSocket server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Scenario> scenarios;
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;
    private int connection;

    public LocalSmtpTestServer(Scenario... scenarios) throws IOException {
        server = new ServerSocket(0, 20, java.net.InetAddress.getLoopbackAddress());
        this.scenarios = new ArrayList<>(List.of(scenarios));
        executor.submit(this::acceptLoop);
    }

    public int port() { return server.getLocalPort(); }
    public List<String> messages() { return List.copyOf(messages); }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = server.accept();
                Scenario scenario;
                synchronized (this) {
                    scenario = scenarios.get(Math.min(connection++, scenarios.size() - 1));
                }
                executor.submit(() -> handle(socket, scenario));
            } catch (IOException ignored) {
                if (running) throw new IllegalStateException(ignored);
            }
        }
    }

    private void handle(Socket socket, Scenario scenario) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
            if (scenario == Scenario.GREETING_TIMEOUT) {
                Thread.sleep(Duration.ofSeconds(2));
                return;
            }
            reply(writer, "220 localhost test SMTP");
            StringBuilder data = new StringBuilder();
            boolean readingData = false;
            for (String line; (line = reader.readLine()) != null;) {
                if (readingData) {
                    if (".".equals(line)) {
                        messages.add(data.toString());
                        readingData = false;
                        reply(writer, "250 2.0.0 accepted-for-delivery");
                    } else {
                        data.append(line).append('\n');
                    }
                } else if (line.startsWith("EHLO") || line.startsWith("HELO")) {
                    if (scenario == Scenario.AUTHENTICATION_REJECTION) {
                        reply(writer, "250-localhost", "250 AUTH LOGIN");
                    } else reply(writer, "250 localhost");
                } else if (line.startsWith("AUTH")) {
                    reply(writer, "535 5.7.8 authentication rejected");
                } else if (line.startsWith("MAIL FROM")) {
                    if (scenario == Scenario.SENDER_REJECTION) reply(writer, "550 5.1.0 sender rejected");
                    else reply(writer, "250 2.1.0 sender accepted");
                } else if (line.startsWith("RCPT TO")) {
                    if (scenario == Scenario.TEMPORARY_RECIPIENT_REJECTION) reply(writer, "450 4.2.0 temporary recipient failure");
                    else if (scenario == Scenario.PERMANENT_RECIPIENT_REJECTION) reply(writer, "550 5.1.1 recipient rejected");
                    else reply(writer, "250 2.1.5 recipient accepted");
                } else if (line.equals("DATA")) {
                    readingData = true;
                    reply(writer, "354 end with dot");
                } else if (line.equals("QUIT")) {
                    reply(writer, "221 bye");
                    return;
                } else reply(writer, "250 ok");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // The client may close a rejected or timed-out SMTP conversation.
        }
    }

    private static void reply(BufferedWriter writer, String... lines) throws IOException {
        for (String line : lines) writer.write(line + "\r\n");
        writer.flush();
    }

    @Override public void close() throws IOException {
        running = false;
        server.close();
        executor.shutdownNow();
    }
}
