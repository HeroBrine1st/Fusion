package ru.herobrine1st.fusion

object Config {
    val googleCustomSearchApiKey: String by lazy { System.getenv("GOOGLE_CUSTOM_SEARCH_API_KEY")!! }
    val googleCustomSearchEngineId: String by lazy { System.getenv("GOOGLE_CUSTOM_SEARCH_ENGINE_ID")!! }
    val youtubeSearchApiKey: String by lazy { System.getenv("YOUTUBE_SEARCH_API_KEY")!! }
    val discordToken: String = System.getenv("DISCORD_TOKEN")!!
    val mysqlUsername: String by lazy { System.getenv("MYSQL_USER")!! }
    val mysqlPassword: String by lazy { System.getenv("MYSQL_PASSWORD")!! }
    val mysqlHost: String by lazy { System.getenv("MYSQL_HOST")!! }
    val mysqlPort: Int = System.getenv("MYSQL_PORT")?.toInt() ?: 3306
    val mysqlDatabase: String by lazy { System.getenv("MYSQL_DATABASE")!! }
    val vkServiceToken: String by lazy { System.getenv("VK_SERVICE_TOKEN")!! }
    val ownerId: String = System.getenv("OWNER_ID")!!
    val testingGuildId: Long? = System.getenv("TESTING_GUILD_ID")?.toLong()
    val maxComponentInteractionWaits: Long? = System.getenv("MAX_COMPONENT_INTERACTION_WAITS")?.toLong()
    val maxComponentInteractionWaitTimeMinutes: Long? = System.getenv("MAX_COMPONENT_INTERACTION_AWAIT_TIME_SECONDS")?.toLong()
    val debug: Boolean = System.getenv("DEBUG") != null
    const val vkAPIVersion: String = "5.124"
}