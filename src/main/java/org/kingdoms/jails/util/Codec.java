package org.kingdoms.jails.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packs records into a single string, so that every piece of data the addon stores goes through
 * the plainest call of the KingdomsX data API: {@code setString} / {@code asString}. Those two are
 * the same in every KingdomsX version and every database backend (YAML, JSON, SQL), which the
 * map and section setters are not.
 * <p>
 * A record is a list of {@code key=value} pairs separated by {@code &}, records are separated by
 * new lines, and every key and value is URL-encoded - a reason containing {@code &} or a line break
 * cannot corrupt anything.
 */
public final class Codec {
    private static final String UTF_8 = "UTF-8";

    private Codec() {}

    public static String encode(Map<String, String> record) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : record.entrySet()) {
            if (entry.getValue() == null) continue;
            if (builder.length() > 0) builder.append('&');
            builder.append(escape(entry.getKey())).append('=').append(escape(entry.getValue()));
        }
        return builder.toString();
    }

    public static Map<String, String> decode(String encoded) {
        Map<String, String> record = new LinkedHashMap<>();
        if (encoded == null || encoded.isEmpty()) return record;

        for (String pair : encoded.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) continue;
            record.put(unescape(pair.substring(0, separator)), unescape(pair.substring(separator + 1)));
        }
        return record;
    }

    public static String encodeAll(List<Map<String, String>> records) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, String> record : records) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(encode(record));
        }
        return builder.toString();
    }

    public static List<Map<String, String>> decodeAll(String encoded) {
        List<Map<String, String>> records = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return records;

        for (String line : encoded.split("\n")) {
            if (line.trim().isEmpty()) continue;
            records.add(decode(line.trim()));
        }
        return records;
    }

    private static String escape(String text) {
        try {
            return URLEncoder.encode(text, UTF_8);
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String unescape(String text) {
        try {
            return URLDecoder.decode(text, UTF_8);
        } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
            return text;
        }
    }
}
