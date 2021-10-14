package ru.herobrine1st.fusion.module.googlesearch;

import net.dv8tion.jda.api.hooks.SubscribeEvent;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.annotation.FusionModule;
import ru.herobrine1st.fusion.api.command.args.GenericArguments;
import ru.herobrine1st.fusion.api.command.build.FusionCommand;
import ru.herobrine1st.fusion.api.event.FusionInitializationEvent;
import ru.herobrine1st.fusion.module.googlesearch.command.ImageCommand;
import ru.herobrine1st.fusion.module.googlesearch.command.YoutubeCommand;

@FusionModule(id="googlesearch")
public class GoogleSearch {
    @SubscribeEvent
    public void onInit(FusionInitializationEvent event) {
        Fusion.getCommandManager().registerCommand(FusionCommand.withArguments("img", "Find image in google")
                .addOptions(GenericArguments.string("query", "Search query"),
                        GenericArguments.string("type", "File type").setRequired(false),
                        GenericArguments.integer("index", "Image index", 0, 9).setRequired(false))
                .setExecutor(new ImageCommand())
                .setShortName("Google Image Search"));

        Fusion.getCommandManager().registerCommand(FusionCommand.withArguments("youtube", "Find video in youtube")
                .addOptions(GenericArguments.string("query", "Search query"),
                        GenericArguments.string("type", "Type of resource. Default: video")
                                .addChoice("video", "video")
                                .addChoice("playlist", "playlist")
                                .addChoice("channel", "channel")
                                .setRequired(false),
                        GenericArguments.integer("index", "Image index", 0, 49).setRequired(false),
                        GenericArguments.integer("max", "Maximum results count", 1, 50).setRequired(false))
                .setExecutor(new YoutubeCommand())
                .setShortName("Youtube Search"));
    }
}
