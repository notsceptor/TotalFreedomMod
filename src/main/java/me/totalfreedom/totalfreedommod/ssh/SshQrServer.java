package me.totalfreedom.totalfreedommod.ssh;

import com.sun.net.httpserver.HttpServer;
import me.totalfreedom.totalfreedommod.util.FLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SshQrServer
{
    private SshQrServer() {}

    public static void serve(String otpUri, String label, int port, String token)
    {
        CompletableFuture.runAsync(() ->
        {
            try
            {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                AtomicBoolean consumed = new AtomicBoolean(false);

                server.createContext("/qr", exchange ->
                {
                    String query = exchange.getRequestURI().getQuery();
                    boolean validToken = query != null && query.contains("t=" + token);

                    if (!validToken || !consumed.compareAndSet(false, true))
                    {
                        byte[] gone = "<html><body><h2>This link has already been used or is invalid.</h2></body></html>"
                                .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                        exchange.sendResponseHeaders(410, gone.length);
                        exchange.getResponseBody().write(gone);
                        exchange.close();
                        return;
                    }

                    byte[] body = buildHtml(otpUri, label).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();

                    scheduler.schedule(() ->
                    {
                        server.stop(0);
                        scheduler.shutdown();
                        FLog.info("[SSH] QR setup server stopped after one-time use.");
                    }, 2, TimeUnit.SECONDS);
                });

                server.start();
                FLog.info("[SSH] QR setup page available on port " + port + " for 5 minutes.");

                scheduler.schedule(() ->
                {
                    server.stop(0);
                    if (!scheduler.isShutdown())
                    {
                        scheduler.shutdown();
                        FLog.info("[SSH] QR setup server timed out.");
                    }
                }, 5, TimeUnit.MINUTES);
            }
            catch (IOException e)
            {
                FLog.warning("[SSH] Failed to start QR server on port " + port + ": " + e.getMessage());
            }
        });
    }

    private static String buildHtml(String otpUri, String label)
    {
        String escaped = otpUri.replace("\\", "\\\\").replace("\"", "\\\"");
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>TFM SSH TOTP Setup</title>"
                + "<style>"
                + "body{font-family:sans-serif;max-width:480px;margin:48px auto;text-align:center;padding:0 16px}"
                + "h2{margin-bottom:8px}p{color:#555}"
                + "#qr{display:inline-block;margin:16px 0}"
                + "code{background:#f4f4f4;padding:6px 10px;border-radius:6px;word-break:break-all;display:block;margin:12px 0;font-size:14px}"
                + "a{color:#1a73e8}"
                + "</style>"
                + "</head><body>"
                + "<h2>TFM SSH — TOTP Setup</h2>"
                + "<p>Scan the QR code with your authenticator app (<strong>" + label + "</strong>).</p>"
                + "<div id=\"qr\"></div>"
                + "<p>Or <a href=\"" + otpUri + "\">tap here</a> to add directly on mobile.</p>"
                + "<p>Manual entry secret:</p><code id=\"sec\"></code>"
                + "<p><small>This page closes after 5 minutes.</small></p>"
                + "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js\"></script>"
                + "<script>"
                + "var u=\"" + escaped + "\";"
                + "new QRCode(document.getElementById(\"qr\"),{text:u,width:256,height:256,correctLevel:QRCode.CorrectLevel.L});"
                + "var m=u.match(/secret=([^&]+)/);if(m)document.getElementById(\"sec\").textContent=m[1];"
                + "</script>"
                + "</body></html>";
    }
}
