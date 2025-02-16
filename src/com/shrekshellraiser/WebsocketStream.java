package com.shrekshellraiser;

import com.shrekshellraiser.formats.IFormat;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebsocketStream extends WebSocketServer {
    private final ImageMaker maker;
    private final Robot robot;
    @Override
    public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int i, String s, boolean b) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + i + " additional info: " + s);
    }

    private String processScreenshot() {
        Rectangle bounds = maker.getRegion();
        if (bounds == null) {
            bounds = new Rectangle(0, 0, 1920, 1080);
        }
        try {
            BufferedImage capture = robot.createScreenCapture(bounds);
            IFormat f = maker.processFrame(new BufferedImage[]{capture});
            return f.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String s) {
        if (s.equals("get")) {
            String screenshot = processScreenshot();
            conn.send(screenshot.getBytes(StandardCharsets.ISO_8859_1));
        } else if (s.startsWith("resolution")) {
            Pattern p = Pattern.compile("resolution(\\d+)x(\\d+)");
            Matcher m = p.matcher(s);
            boolean matchFound = m.find();
            String width = m.group(1);
            String height = m.group(2);
            maker.setSize(Integer.parseInt(width), Integer.parseInt(height));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        System.err.println("an error occurred on connection:" + e);
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
    }

    public WebsocketStream(int port, ImageMaker maker) {
        super(new InetSocketAddress("localhost", port));
        this.maker = maker;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
}
