package ru.herobrine1st.fusion.util;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ModifiedEmbedBuilder extends EmbedBuilder {
    @NotNull
    @Override
    public EmbedBuilder setTitle(@Nullable String title, @Nullable String url) {
        if(title == null && url != null) {
            try {
                Field titleField = EmbedBuilder.class.getDeclaredField("title");
                titleField.setAccessible(true);
                titleField.set(this, null);
                Field urlField = EmbedBuilder.class.getDeclaredField("url");
                urlField.setAccessible(true);
                urlField.set(this, url);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return this;
        } else {
            return super.setTitle(title, url);
        }
    }
}
