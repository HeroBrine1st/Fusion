package ru.herobrine1st.fusion

object Config {
    val googleCustomSearchApiKey: String = System.getenv("GOOGLE_CUSTOM_SEARCH_API_KEY")!!
    val googleCustomSearchEngineId: String = System.getenv("GOOGLE_CUSTOM_SEARCH_ENGINE_ID")!!
    val youtubeSearchApiKey: String = System.getenv("YOUTUBE_SEARCH_API_KEY")!!
    val discordToken: String = System.getenv("DISCORD_TOKEN")!!
    val mysqlUsername: String = System.getenv("MYSQL_USER")!!
    val mysqlPassword: String = System.getenv("MYSQL_PASSWORD")!!
    val mysqlHost: String = System.getenv("MYSQL_HOST")!!
    val mysqlPort: Int = System.getenv("MYSQL_PORT")!!.toInt()
    val mysqlDatabase: String = System.getenv("MYSQL_DATABASE")!!
    val vkServiceToken: String = System.getenv("VK_SERVICE_TOKEN")!!
    val ownerId: String = System.getenv("OWNER_ID")!!
    val testingGuildId: Long? = System.getenv("TESTING_GUILD_ID")?.toLong()
    val maxComponentInteractionAwaits: Long? = System.getenv("MAX_COMPONENT_INTERACTION_AWAITS")?.toLong()
    const val vkAPIVersion: String = "5.124"
}