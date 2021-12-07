package ru.herobrine1st.fusion;

public final class Config {

    private Config() {
    }

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

    public static String getMysqlUsername() {
        return System.getenv("MYSQL_USER");
    }

    public static String getMysqlPassword() {
        return System.getenv("MYSQL_PASSWORD");
    }

    public static String getMysqlHost() {
        return System.getenv("MYSQL_HOST");
    }

    public static int getMysqlPort() {
        return Integer.parseInt(System.getenv("MYSQL_PORT"));
    }

    public static String getMysqlDatabase() {
        return System.getenv("MYSQL_DATABASE");
    }

    public static String getVkServiceToken() {
        return System.getenv("VK_SERVICE_TOKEN");
    }

    public static String getVkAPIVersion() {
        return "5.124";
    }

    public static String getOwnerId() {
        return System.getenv("OWNER_ID");
    }

}
