package com.magmaguy.betterstructures.commands;

import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import java.util.List;

public class UpdateContentCommand extends AdvancedCommand {
    public UpdateContentCommand() {
        super(List.of("updatecontent", "updateall"));
        setPermission("betterstructures.setup");
        setSenderType(SenderType.ANY);
        setDescription("Downloads updates for outdated BetterStructures Nightbreak content.");
        setUsage("/bs updatecontent");
    }

    @Override
    public void execute(CommandData commandData) {
        DownloadAllContentCommand.execute(commandData.getCommandSender(), true);
    }
}
