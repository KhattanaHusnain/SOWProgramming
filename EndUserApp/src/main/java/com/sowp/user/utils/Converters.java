package com.sowp.user.utils;

import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Converters {

    // Convert List<String> to a single String (comma separated)
    @TypeConverter
    public String fromList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    // Convert a single String back to List<String>
    @TypeConverter
    public List<String> fromString(String value) {
        if (value == null || value.isEmpty()) return null;
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
