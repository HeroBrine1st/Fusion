package ru.herobrine1st.fusion.util

import com.mysql.cj.jdbc.Driver
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import ru.herobrine1st.fusion.Config
import ru.herobrine1st.fusion.module.vk.entity.VkGroupEntity
import ru.herobrine1st.fusion.module.vk.entity.VkGroupSubscriberEntity



fun constructHibernateConfiguration(): Configuration {
    return Configuration()
        .setProperty(
            Environment.URL,
            "jdbc:mysql://%s:%s@%s:%s/%s".format(
                Config.mysqlUsername,
                Config.mysqlPassword,
                Config.mysqlHost,
                Config.mysqlPort,
                Config.mysqlDatabase
            )
        )
        .setProperty(Environment.DRIVER, Driver::class.java.canonicalName)
        .setProperty(Environment.HBM2DDL_AUTO, "update") // TODO somehow export sql files and then set to "validate"
        // vk
        .addAnnotatedClass(VkGroupEntity::class.java)
        .addAnnotatedClass(VkGroupSubscriberEntity::class.java)
}