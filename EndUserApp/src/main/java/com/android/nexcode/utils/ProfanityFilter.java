package com.android.nexcode.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfanityFilter {

    private Set<String> profanityList;

    public ProfanityFilter() {
        initializeProfanityList();
    }

    private void initializeProfanityList() {
        // Add common profanity words (you can expand this list)
        profanityList = new HashSet<>(Arrays.asList(
                "damn", "hell", "shit", "fuck", "bitch", "ass", "bastard",
                "crap", "piss", "dick", "cock", "pussy", "whore", "slut",
                "dumbass", "jackass", "motherfucker", "asshole", "bullshit",
                "stupid", "idiot", "moron", "retard", "gay", "fag"
        ));
    }

    public String filter(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String filteredMessage = message;

        // Create a case-insensitive pattern for each profanity word
        for (String profanity : profanityList) {
            // Create pattern that matches the word with word boundaries
            // This prevents partial matches (e.g., "ass" in "class")
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(profanity) + "\\b",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(filteredMessage);

            // Replace with asterisks of the same length
            while (matcher.find()) {
                String replacement = generateAsterisks(matcher.group().length());
                filteredMessage = filteredMessage.substring(0, matcher.start()) +
                        replacement +
                        filteredMessage.substring(matcher.end());
                // Reset matcher with new string
                matcher = pattern.matcher(filteredMessage);
            }
        }

        return filteredMessage;
    }

    private String generateAsterisks(int length) {
        StringBuilder asterisks = new StringBuilder();
        for (int i = 0; i < length; i++) {
            asterisks.append("*");
        }
        return asterisks.toString();
    }

}