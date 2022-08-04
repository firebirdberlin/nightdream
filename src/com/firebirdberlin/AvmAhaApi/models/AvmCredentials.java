package com.firebirdberlin.AvmAhaApi.models;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AvmCredentials {

    public String host;
    public String username;
    public String password;

    public AvmCredentials(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }


    public String getSecret(String challenge) {
        String to_hash = challenge + "-" + password;
        Charset utf16 = Charset.forName("UTF-16LE");
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(to_hash.getBytes(utf16));
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            String hashed = hexString.toString();
            return String.format("%s-%s", challenge, hashed);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
