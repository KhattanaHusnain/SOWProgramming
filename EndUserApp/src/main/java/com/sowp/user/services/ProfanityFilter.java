package com.sowp.user.services;

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
                "stupid", "idiot", "moron", "retard", "gay", "fag", "wtf", "fck",
                "bloody", "bugger", "sod", "prick", "twat", "cunt", "wanker",
                "bollocks", "tits", "shag", "slag", "git", "tosser", "pillock",
                "bsdk", "bhosdk", "bhosadi", "madarchod", "mc", "bc", "behenchod",
                "sisterfucker", "chutiya", "chutiye", "chut", "lund", "lun", "lan",
                "gaand", "gand", "gandu", "gaandu", "randi", "randwa", "raand",
                "harami", "haramzada", "kamina", "kamine", "saala", "saali",
                "kutiya", "kutta", "kutte", "gandoo", "hijra", "chakka",
                "phuddi", "phudi", "kus", "kuss", "gashti", "gashtee", "pendu",
                "jhatka", "bhenchod", "penchod", "mundya", "kanjri", "kanjr",
                "lurha", "bakrichod", "jhant", "jhanta", "bund", "bhund",
                "lundure", "randiya", "khota", "khotay", "kameena", "kameeni",
                "bhenchod", "benchod", "maderjaat", "ullu", "gadha", "gadhe",
                "kutia", "kaminey", "haramkhor", "najayaz", "badmaash",
                "gobar", "tatti", "haggu", "mut", "mutth", "moot", "peshaab",
                "sandas", "potty", "suwar", "suar", "kutta", "billa", "bandar",
                "sex", "boob", "boobs", "breast", "nipple", "vagina", "penis",
                "orgasm", "masturbate", "porn", "xxx", "nude", "naked",
                "lingam", "yoni", "kamsutra", "jism", "virya", "dhaat",
                "stupid", "dumb", "fool", "foolish", "pagal", "mental", "crack",
                "nuts", "crazy", "bevakoof", "ullu", "buddhu", "nalayak",
                "saala", "haramzada", "lanat", "kafir", "mlechha",
                "f*ck", "f**k", "sh*t", "b*tch", "a**hole", "d*mn",
                "fuk", "fuq", "shyt", "sht", "btch", "azz", "asz",
                "phuck", "phuk", "shiit", "biatch", "beetch", "daam",
                "f0ck", "sh1t", "b1tch", "a55", "a55hole", "d4mn",
                "fvck", "shvt", "bvtch", "cvnt", "dvck", "pvss",
                "bhosdike", "lavde", "lavda", "teri", "maa", "bahen",
                "beti", "randi", "chinaal", "tawaif", "pataka", "item",
                "magi", "maggi", "rand", "bhikhari", "hijde", "kinnar"
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