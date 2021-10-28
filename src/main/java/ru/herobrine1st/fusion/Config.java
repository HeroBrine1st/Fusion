package ru.herobrine1st.fusion;

public final class Config {
    private Config() {}

    public static String getGoogleCustomSearchApiKey() {
        return System.getenv("GOOGLE_CUSTOM_SEARCH_API_KEY");
    }

    public static String getGoogleCustomSearchEngineId() {
        return System.getenv("GOOGLE_CUSTOM_SEARCH_ENGINE_ID");
    }

    public static String getYoutubeSearchApiKey() {
        return System.getenv("YOUTUBE_SEARCH_API_KEY");
    }

    public static String getDiscordToken() {
        return System.getenv("DISCORD_TOKEN");
    }
}
