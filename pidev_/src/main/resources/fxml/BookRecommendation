package models;

import java.util.*;

public class CompletionData {

    private static final Map<String, List<String>> completions = new HashMap<>();

    static {
        completions.put("I feel", List.of("i feel happy today", "i feel a bit tired", "i feel excited"));
        completions.put("Today I", List.of(" Today I went to the park", "Today I finished my work", "Today I read a book"));
        completions.put("My goal", List.of("My goal is to exercise", "My goal is to finish a book", "My goal is to meditate"));
        completions.put("I want", List.of("I want to learn Java", "I want to travel", "I want to write more"));
        // add more keyword → suggestions here
    }

    public static List<String> getSuggestions(String prefix) {
        List<String> suggestions = new ArrayList<>();
        completions.forEach((key, valueList) -> {
            if (prefix.toLowerCase().startsWith(key.toLowerCase())) {
                suggestions.addAll(valueList);
            }
        });
        return suggestions;
    }
}