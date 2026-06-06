package com.ruskserver.deepwither_V2.modules.character;

public enum CharacterMode {
    STANDARD("スタンダード"),
    SOFT_HARDCORE("ソフトHC"),
    TRUE_HARDCORE("真HC");

    private final String displayName;

    CharacterMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNamePrefix() {
        return switch (this) {
            case STANDARD -> "";
            case SOFT_HARDCORE -> "[SHC] ";
            case TRUE_HARDCORE -> "[THC] ";
        };
    }

    public boolean usesSharedProgress() {
        return this == STANDARD || this == SOFT_HARDCORE;
    }

    public boolean isHardcore() {
        return this == SOFT_HARDCORE || this == TRUE_HARDCORE;
    }

    public static CharacterMode parse(String raw) {
        String normalized = raw.toLowerCase(java.util.Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "standard", "std", "normal", "スタンダード" -> STANDARD;
            case "soft", "soft_hardcore", "shc", "ソフト", "ソフトhc" -> SOFT_HARDCORE;
            case "true", "true_hardcore", "thc", "hardcore", "真hc", "真ハードコア" -> TRUE_HARDCORE;
            default -> throw new IllegalArgumentException("Unknown character mode: " + raw);
        };
    }
}
