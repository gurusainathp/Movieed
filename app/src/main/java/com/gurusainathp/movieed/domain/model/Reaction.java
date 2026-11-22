package com.gurusainathp.movieed.domain.model;

public enum Reaction {
    LOVE("LOVE", 3.0f, "😍", "😍 Love"),
    LIKE("LIKE", 1.5f, "🙂", "🙂 Like"),
    UMM("UMM", 0.0f, "😐", "😐 Umm"),
    DISLIKE("DISLIKE", -1.5f, "🙁", "🙁 Dislike"),
    HATE("HATE", -3.0f, "😡", "😡 Hate");

    private final String rawValue;
    private final float weight;
    private final String emoji;
    private final String label;

    Reaction(String rawValue, float weight, String emoji, String label) {
        this.rawValue = rawValue;
        this.weight = weight;
        this.emoji = emoji;
        this.label = label;
    }

    public String getRawValue() {
        return rawValue;
    }

    public float getWeight() {
        return weight;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getLabel() {
        return label;
    }

    public static Reaction fromRawString(String raw) {
        if (raw == null) return null;
        for (Reaction r : values()) {
            if (r.rawValue.equalsIgnoreCase(raw)) {
                return r;
            }
        }
        return null;
    }

    public static float getWeight(String raw) {
        Reaction r = fromRawString(raw);
        return r != null ? r.getWeight() : 0.0f;
    }
}
