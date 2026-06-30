package ru.roughcipher.pidge.util;

import java.util.ArrayList;
import java.util.List;

public class MessageUtils {
    public static List<String> splitMessage(String text, int limit) {
        List<String> parts = new ArrayList<>();
        if (text.length() <= limit) {
            parts.add(text);
            return parts;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + limit, text.length());
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            parts.add(text.substring(start, end));
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') start++;
        }
        return parts;
    }
}