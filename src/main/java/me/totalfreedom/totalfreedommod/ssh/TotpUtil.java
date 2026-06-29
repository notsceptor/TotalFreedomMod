package me.totalfreedom.totalfreedommod.ssh;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class TotpUtil
{
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] BASE32 = new int[256];
    private static final long STEP = 30L;

    static
    {
        java.util.Arrays.fill(BASE32, -1);
        for (int i = 0; i < ALPHABET.length(); i++)
        {
            BASE32[ALPHABET.charAt(i)] = i;
        }
    }

    private TotpUtil() {}

    public static String generateSecret()
    {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return toBase32(bytes);
    }

    public static String buildUri(String issuer, String label, String secret)
    {
        String encodedLabel = URLEncoder.encode(issuer + ":" + label, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + encodedLabel
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    static String toBase32(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        int buf = 0, bits = 0;
        for (byte b : data)
        {
            buf = (buf << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5)
            {
                sb.append(ALPHABET.charAt((buf >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0)
        {
            sb.append(ALPHABET.charAt((buf << (5 - bits)) & 0x1F));
        }
        while (sb.length() % 8 != 0)
        {
            sb.append('=');
        }
        return sb.toString();
    }

    public static boolean verify(String base32Secret, String code)
    {
        if (base32Secret == null || code == null)
        {
            return false;
        }
        try
        {
            byte[] key = decode(base32Secret.toUpperCase().replaceAll("[^A-Z2-7=]", ""));
            long timeStep = System.currentTimeMillis() / 1000L / STEP;
            for (long t = timeStep - 1; t <= timeStep + 1; t++)
            {
                if (totp(key, t).equals(code.trim()))
                {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static String totp(byte[] key, long step) throws Exception
    {
        byte[] data = ByteBuffer.allocate(8).putLong(step).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0xf;
        int code = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        return String.format("%06d", code % 1_000_000);
    }

    private static byte[] decode(String input)
    {
        int len = input.replace("=", "").length();
        byte[] out = new byte[len * 5 / 8];
        int buf = 0, bits = 0, idx = 0;
        for (char c : input.toCharArray())
        {
            if (c == '=') break;
            int val = BASE32[c];
            if (val < 0) continue;
            buf = (buf << 5) | val;
            bits += 5;
            if (bits >= 8)
            {
                out[idx++] = (byte) (buf >> (bits - 8));
                bits -= 8;
            }
        }
        return out;
    }
}
