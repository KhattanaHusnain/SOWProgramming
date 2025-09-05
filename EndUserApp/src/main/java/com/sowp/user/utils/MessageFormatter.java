package com.sowp.user.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.sowp.user.models.Topic;
import com.sowp.user.presenters.activities.TopicView;
import com.sowp.user.repositories.firebase.TopicRepository;

import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {

    // Regex patterns for different formatting

    TopicRepository repository;
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.*?)__");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.*?)~~");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":(\\w+):");

    // Common emoji mappings
    private static final String[][] EMOJI_MAP = {
            {":)", "😊"}, {":D", "😃"}, {":(", "😢"}, {":P", "😛"},
            {";)", "😉"}, {":'(", "😭"}, {":o", "😮"}, {":|", "😐"},
            {"<3", "❤️"}, {"</3", "💔"}, {":*", "😘"}, {"^_^", "😊"},
            {"-_-", "😑"}, {">:(", "😠"}, {":@", "😡"}, {"XD", "😆"},
            {"lol", "😂"}, {"LOL", "😂"}, {"omg", "😱"}, {"OMG", "😱"}
    };

    /**
     * Main method to format a message with basic text formatting
     * @param message The raw message text
     * @return SpannableString with applied formatting
     */
    public static SpannableString formatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new SpannableString("");
        }

        // Start with the original message
        String formattedText = message;

        // Apply emoji replacements first
        formattedText = replaceEmojis(formattedText);

        // Create SpannableStringBuilder for applying styles
        SpannableStringBuilder builder = new SpannableStringBuilder(formattedText);

        // Apply various formatting styles
        applyBoldFormatting(builder);
        applyItalicFormatting(builder);
        applyUnderlineFormatting(builder);
        applyStrikethroughFormatting(builder);
        applyCodeFormatting(builder);

        return new SpannableString(builder);
    }

    /**
     * Replace text emojis with Unicode emojis
     */
    private static String replaceEmojis(String text) {
        String result = text;
        for (String[] emoji : EMOJI_MAP) {
            result = result.replace(emoji[0], emoji[1]);
        }
        return result;
    }

    /**
     * Apply bold formatting for **text**
     */
    private static void applyBoldFormatting(SpannableStringBuilder builder) {
        Matcher matcher = BOLD_PATTERN.matcher(builder.toString());

        // Process matches in reverse order to maintain indices
        int adjustment = 0;
        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            // Replace the markdown with just the content
            builder.replace(start, end, content);

            // Apply bold style
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Adjust for the removed markdown characters
            adjustment += 4; // ** at start and end
        }
    }

    /**
     * Apply italic formatting for *text*
     */
    private static void applyItalicFormatting(SpannableStringBuilder builder) {
        Matcher matcher = ITALIC_PATTERN.matcher(builder.toString());

        int adjustment = 0;
        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            // Skip if this is part of a bold formatting (already processed)
            if (start > 0 && builder.charAt(start - 1) == '*') {
                continue;
            }
            if (end < builder.length() && builder.charAt(end) == '*') {
                continue;
            }

            builder.replace(start, end, content);
            builder.setSpan(new StyleSpan(Typeface.ITALIC),
                    start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            adjustment += 2; // * at start and end
        }
    }

    /**
     * Apply underline formatting for __text__
     */
    private static void applyUnderlineFormatting(SpannableStringBuilder builder) {
        Matcher matcher = UNDERLINE_PATTERN.matcher(builder.toString());

        int adjustment = 0;
        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            builder.replace(start, end, content);
            builder.setSpan(new UnderlineSpan(),
                    start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            adjustment += 4; // __ at start and end
        }
    }

    /**
     * Apply strikethrough formatting for ~~text~~
     */
    private static void applyStrikethroughFormatting(SpannableStringBuilder builder) {
        Matcher matcher = STRIKETHROUGH_PATTERN.matcher(builder.toString());

        int adjustment = 0;
        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            builder.replace(start, end, content);
            builder.setSpan(new StrikethroughSpan(),
                    start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            adjustment += 4; // ~~ at start and end
        }
    }

    /**
     * Apply code formatting for `text` (monospace)
     */
    private static void applyCodeFormatting(SpannableStringBuilder builder) {
        Matcher matcher = CODE_PATTERN.matcher(builder.toString());

        int adjustment = 0;
        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            builder.replace(start, end, content);
            builder.setSpan(new TypefaceSpan("monospace"),
                    start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            adjustment += 2; // ` at start and end
        }
    }

    /**
     * Format message with automatic link detection
     * @param message The message to format
     * @return SpannableString with links and formatting
     */
    public static SpannableString formatMessageWithLinks(String message) {
        SpannableString formatted = formatMessage(message);

        // Add automatic link detection
        Linkify.addLinks(formatted, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

        return formatted;
    }

    /**
     * Get plain text version of formatted message (removes formatting)
     * @param message Message with formatting
     * @return Plain text without formatting markers
     */
    public static String getPlainText(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String plain = message;

        // Remove formatting markers
        plain = plain.replaceAll("\\*\\*(.*?)\\*\\*", "$1"); // Bold
        plain = plain.replaceAll("(?<!\\*)\\*(.*?)\\*(?!\\*)", "$1"); // Italic
        plain = plain.replaceAll("__(.*?)__", "$1"); // Underline
        plain = plain.replaceAll("~~(.*?)~~", "$1"); // Strikethrough
        plain = plain.replaceAll("`(.*?)`", "$1"); // Code

        return plain;
    }

    /**
     * Check if message contains formatting
     * @param message Message to check
     * @return true if message contains formatting syntax
     */
    public static boolean hasFormatting(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        return BOLD_PATTERN.matcher(message).find() ||
                ITALIC_PATTERN.matcher(message).find() ||
                UNDERLINE_PATTERN.matcher(message).find() ||
                STRIKETHROUGH_PATTERN.matcher(message).find() ||
                CODE_PATTERN.matcher(message).find();
    }

    /**
     * Escape formatting characters to display them literally
     * @param message Message to escape
     * @return Message with escaped formatting characters
     */
    public static String escapeFormatting(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }

        return message.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`");
    }


    public static String previewFormatting(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String preview = replaceEmojis(message);

        // Replace formatting with visual indicators
        preview = preview.replaceAll("\\*\\*(.*?)\\*\\*", "𝐁$1"); // Bold indicator
        preview = preview.replaceAll("(?<!\\*)\\*(.*?)\\*(?!\\*)", "𝐼$1"); // Italic indicator
        preview = preview.replaceAll("__(.*?)__", "U̲$1"); // Underline indicator
        preview = preview.replaceAll("~~(.*?)~~", "S̶t̶r̶i̶k̶e̶$1"); // Strikethrough indicator
        preview = preview.replaceAll("`(.*?)`", "「$1」"); // Code indicator

        return preview;
    }

    public SpannableString makeLinksClickable(String message, Context context) {
        SpannableString spannable = new SpannableString(message);

        // Match Course/x/Topics/y
        Pattern pattern = Pattern.compile("Course/(\\d+)/Topics/(\\d+)");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            final String courseId = matcher.group(1);
            final String topicId = matcher.group(2);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {

                    repository.loadTopicsOfCourse(Integer.parseInt(courseId), new TopicRepository.Callback() {
                        @Override
                        public void onSuccess(List<Topic> topics) {


                            Intent intent = new Intent(context, TopicView.class);
                            intent.putExtra("TOPIC_NAME", Integer.parseInt(courseId));
                            intent.putExtra("TOPIC_CONTENT", Integer.parseInt(courseId));
                            intent.putExtra("VIDEO_ID", Integer.parseInt(courseId));
                            context.startActivity(intent);

                        }

                        @Override
                        public void onFailure(String message) {

                        }
                    });

                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.BLUE); // make link blue
                    ds.setUnderlineText(true); // underline like hyperlink
                }
            };

            spannable.setSpan(clickableSpan,
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }


}