package com.sowp.user.services;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sowp.user.models.Topic;
import com.sowp.user.presenters.activities.TopicView;
import com.sowp.user.presenters.activities.TakeQuizActivity;
import com.sowp.user.presenters.activities.SubmitAssignmentActivity;
import com.sowp.user.repositories.TopicRepository;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("__(.*?)__");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(.*?)~~");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`");

    private static final Pattern TOPIC_PATTERN = Pattern.compile("Course/(\\d+)/Topics/(\\d+)");
    private static final Pattern QUIZ_PATTERN = Pattern.compile("Course/(\\d+)/Quizzes/(\\d+)");
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("Course/(\\d+)/Assignments/(\\d+)");

    private static final int TOPIC_COLOR = Color.BLACK;
    private static final int QUIZ_COLOR = Color.BLACK;
    private static final int ASSIGNMENT_COLOR = Color.BLACK;

    private static final Map<String, String> EMOJI_MAP = new HashMap<String, String>() {{
        put(":)", "😊"); put(":D", "😃"); put(":(", "😢"); put(":P", "😛");
        put(";)", "😉"); put(":'(", "😭"); put(":o", "😮"); put(":|", "😐");
        put("<3", "❤️"); put("</3", "💔"); put(":*", "😘"); put("^_^", "😊");
        put("-_-", "😑"); put(">:(", "😠"); put(":@", "😡"); put("XD", "😆");
        put("lol", "😂"); put("LOL", "😂"); put("omg", "😱"); put("OMG", "😱");
    }};

    private static final class FormatConfig {
        final Pattern pattern;
        final Object span;
        final int markerLength;

        FormatConfig(Pattern pattern, Object span, int markerLength) {
            this.pattern = pattern;
            this.span = span;
            this.markerLength = markerLength;
        }
    }

    private static final FormatConfig[] FORMAT_CONFIGS = {
            new FormatConfig(BOLD_PATTERN, new StyleSpan(Typeface.BOLD), 4),
            new FormatConfig(UNDERLINE_PATTERN, new UnderlineSpan(), 4),
            new FormatConfig(STRIKETHROUGH_PATTERN, new StrikethroughSpan(), 4),
            new FormatConfig(CODE_PATTERN, new TypefaceSpan("monospace"), 2)
    };

    private final TopicRepository repository;

    public MessageFormatter() {
        this.repository = new TopicRepository();
    }

    public MessageFormatter(TopicRepository repository) {
        this.repository = repository;
    }

    public SpannableString formatComplete(@Nullable String message, @Nullable Context context) {
        if (isNullOrEmpty(message)) {
            return new SpannableString("");
        }

        try {
            SpannableString formatted = applyTextFormatting(message);
            Linkify.addLinks(formatted, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

            if (context != null) {
                formatted = addCustomLinks(formatted, context);
            }

            return formatted;

        } catch (Exception e) {
            return new SpannableString(message);
        }
    }

    public static SpannableString formatTextOnly(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return new SpannableString("");
        }

        try {
            SpannableString formatted = applyTextFormatting(message);
            Linkify.addLinks(formatted, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
            return formatted;
        } catch (Exception e) {
            return new SpannableString(message);
        }
    }

    public static SpannableString formatBasic(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return new SpannableString("");
        }

        try {
            return applyTextFormatting(message);
        } catch (Exception e) {
            return new SpannableString(message);
        }
    }

    private static SpannableString applyTextFormatting(String message) {
        String processedText = replaceEmojis(message);
        SpannableStringBuilder builder = new SpannableStringBuilder(processedText);

        for (FormatConfig config : FORMAT_CONFIGS) {
            applyPatternFormatting(builder, config);
        }

        applyItalicFormatting(builder);

        return new SpannableString(builder);
    }

    private static String replaceEmojis(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static void applyPatternFormatting(SpannableStringBuilder builder, FormatConfig config) {
        Matcher matcher = config.pattern.matcher(builder.toString());
        int adjustment = 0;

        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            builder.replace(start, end, content);
            builder.setSpan(createSpanInstance(config.span), start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            adjustment += config.markerLength;
        }
    }

    private static void applyItalicFormatting(SpannableStringBuilder builder) {
        Matcher matcher = ITALIC_PATTERN.matcher(builder.toString());
        int adjustment = 0;

        while (matcher.find()) {
            int start = matcher.start() - adjustment;
            int end = matcher.end() - adjustment;
            String content = matcher.group(1);

            if (isPartOfBoldFormatting(builder, start, end)) {
                continue;
            }

            builder.replace(start, end, content);
            builder.setSpan(new StyleSpan(Typeface.ITALIC), start, start + content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            adjustment += 2;
        }
    }

    private static boolean isPartOfBoldFormatting(SpannableStringBuilder builder, int start, int end) {
        return (start > 0 && builder.charAt(start - 1) == '*') ||
                (end < builder.length() && builder.charAt(end) == '*');
    }

    private static Object createSpanInstance(Object template) {
        if (template instanceof StyleSpan) {
            return new StyleSpan(((StyleSpan) template).getStyle());
        } else if (template instanceof UnderlineSpan) {
            return new UnderlineSpan();
        } else if (template instanceof StrikethroughSpan) {
            return new StrikethroughSpan();
        } else if (template instanceof TypefaceSpan) {
            return new TypefaceSpan(((TypefaceSpan) template).getFamily());
        }
        return template;
    }

    private SpannableString addCustomLinks(SpannableString spannable, Context context) {
        try {
            WeakReference<Context> contextRef = new WeakReference<>(context);

            spannable = addLinksByPattern(spannable, TOPIC_PATTERN, TOPIC_COLOR, contextRef, this::loadAndOpenTopic);
            spannable = addLinksByPattern(spannable, QUIZ_PATTERN, QUIZ_COLOR, contextRef, this::openQuiz);
            spannable = addLinksByPattern(spannable, ASSIGNMENT_PATTERN, ASSIGNMENT_COLOR, contextRef, this::openAssignment);

            return spannable;
        } catch (Exception e) {
            return spannable;
        }
    }

    private SpannableString addLinksByPattern(SpannableString spannable, Pattern pattern, int color,
                                              WeakReference<Context> contextRef, LinkClickHandler handler) {
        String text = spannable.toString();
        Matcher matcher = pattern.matcher(text);
        SpannableStringBuilder builder = new SpannableStringBuilder(spannable);

        while (matcher.find()) {
            final String courseId = matcher.group(1);
            final String itemId = matcher.group(2);
            final String linkText = matcher.group(0);

            ClickableSpan clickableSpan = createClickableSpan(contextRef, courseId, itemId, linkText, color, handler);
            builder.setSpan(clickableSpan, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return new SpannableString(builder);
    }

    private ClickableSpan createClickableSpan(WeakReference<Context> contextRef, String courseId,
                                              String itemId, String linkText, int color, LinkClickHandler handler) {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Context context = contextRef.get();
                if (context == null) {
                    return;
                }

                handler.handleClick(courseId, itemId, context);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(color);
                ds.setUnderlineText(true);
            }
        };
    }

    @FunctionalInterface
    private interface LinkClickHandler {
        void handleClick(String courseId, String itemId, Context context);
    }

    private void loadAndOpenTopic(String courseId, String topicId, Context context) {
        try {
            int courseIdInt = Integer.parseInt(courseId);
            int topicIdInt = Integer.parseInt(topicId);

            repository.getTopicById(courseIdInt, topicIdInt, new TopicRepository.SingleTopicCallback() {
                @Override
                public void onSuccess(Topic topic) {
                    openTopicActivity(context, topic);
                }

                @Override
                public void onFailure(String errorMessage) {
                }
            });

        } catch (NumberFormatException e) {
        }
    }

    private void openTopicActivity(Context context, Topic topic) {
        try {
            Intent intent = new Intent(context, TopicView.class);
            intent.putExtra("TOPIC_NAME", topic.getName());
            intent.putExtra("TOPIC_CONTENT", topic.getContent());
            intent.putExtra("VIDEO_ID", topic.getVideoID());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void openQuiz(String courseId, String quizId, Context context) {
        try {
            Intent intent = new Intent(context, TakeQuizActivity.class);
            intent.putExtra("COURSE_ID", courseId);
            intent.putExtra("QUIZ_ID", quizId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void openAssignment(String courseId, String assignmentId, Context context) {
        try {
            int courseIdInt = Integer.parseInt(courseId);
            int assignmentIdInt = Integer.parseInt(assignmentId);

            Intent intent = new Intent(context, SubmitAssignmentActivity.class);
            intent.putExtra("COURSE_ID", courseIdInt);
            intent.putExtra("ASSIGNMENT_ID", assignmentIdInt);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (NumberFormatException e) {
        }
    }

    public static String getPlainText(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return "";
        }

        return message
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("(?<!\\*)\\*(.*?)\\*(?!\\*)", "$1")
                .replaceAll("__(.*?)__", "$1")
                .replaceAll("~~(.*?)~~", "$1")
                .replaceAll("`(.*?)`", "$1");
    }

    public static boolean hasFormatting(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return false;
        }

        return BOLD_PATTERN.matcher(message).find() ||
                ITALIC_PATTERN.matcher(message).find() ||
                UNDERLINE_PATTERN.matcher(message).find() ||
                STRIKETHROUGH_PATTERN.matcher(message).find() ||
                CODE_PATTERN.matcher(message).find();
    }

    public static String escapeFormatting(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return message != null ? message : "";
        }

        return message.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`");
    }

    public static String previewFormatting(@Nullable String message) {
        if (isNullOrEmpty(message)) {
            return "";
        }

        String preview = replaceEmojis(message);

        return preview.replaceAll("\\*\\*(.*?)\\*\\*", "𝐁$1")
                .replaceAll("(?<!\\*)\\*(.*?)\\*(?!\\*)", "𝐼$1")
                .replaceAll("__(.*?)__", "U̲$1")
                .replaceAll("~~(.*?)~~", "S̶t̶r̶i̶k̶e̶$1")
                .replaceAll("`(.*?)`", "「$1」");
    }

    private static boolean isNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }
}