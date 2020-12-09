package fr.moribus.imageonmap.commands.maptool;

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.commands.IoMCommand;
import fr.moribus.imageonmap.image.ImageRendererExecutor;
import fr.moribus.imageonmap.image.ImageUtils;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.worker.WorkerCallback;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.text.ActionBar;
import fr.zcraft.quartzlib.tools.text.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@CommandInfo (name = "latex", usageParameters = "<LaTeX Code>")
public class LatexCommand  extends IoMCommand
{
    @Override
    protected void run() throws CommandException
    {
        final Player player = playerSender();
        ImageUtils.ScalingType scaling = ImageUtils.ScalingType.NONE;
        String latexRaw;
        int width = 0, height = 0;

        if(args.length < 1) throwInvalidArgument(I.t("You must provide LaTeX code."));

        if(args.length >= 4){
            if(args[args.length-3].equals("resize") || args[args.length-3].equals("resize-stretched") || args[args.length-3].equals("resize-covered")){
                player.sendMessage("Detected resize request: " + args[args.length-2] + "x" + args[args.length-1]);
                switch(args[args.length-3]) {
                    case "resize": scaling = ImageUtils.ScalingType.CONTAINED; break;
                    case "resize-stretched": scaling = ImageUtils.ScalingType.STRETCHED; break;
                    case "resize-covered": scaling = ImageUtils.ScalingType.COVERED; break;
                    default: throwInvalidArgument(I.t("Invalid Stretching mode.")); break;
                }

                width = Integer.parseInt(args[args.length-2]);
                height = Integer.parseInt(args[args.length-1]);
                String[] new_args = Arrays.copyOfRange(args, 0, args.length-3);

                latexRaw = StringUtils.join(new_args, " ");
                player.sendMessage("Parsing the following: " + latexRaw);
            }
            else{
                latexRaw = StringUtils.join(args, " ");
            }
        }
        else {
            latexRaw = StringUtils.join(args, " ");
        }
        //Don't have a good way to reconcile this with how I get the raw LaTeX code...
        /*
        if(args.length >= 2)
        {
            if(args.length >= 4) {
                width = Integer.parseInt(args[2]);
                height = Integer.parseInt(args[3]);
            }

            switch(args[1]) {
                case "resize": scaling = ImageUtils.ScalingType.CONTAINED; break;
                case "resize-stretched": scaling = ImageUtils.ScalingType.STRETCHED; break;
                case "resize-covered": scaling = ImageUtils.ScalingType.COVERED; break;
                default: throwInvalidArgument(I.t("Invalid Stretching mode.")); break;
            }
        }
         */

        try {


            ActionBar.sendPermanentMessage(player, ChatColor.DARK_GREEN + I.t("Rendering..."));
            ImageRendererExecutor.renderLatex(latexRaw, player, scaling, player.getUniqueId(), width, height, new WorkerCallback<ImageMap>() {
                @Override
                public void finished(ImageMap result) {
                    ActionBar.removeMessage(player);
                    MessageSender.sendActionBarMessage(player, ChatColor.DARK_GREEN + I.t("Rendering shmendering!"));

                    if (result.give(player) && (result instanceof PosterMap && !((PosterMap) result).hasColumnData())) {
                        info(I.t("The rendered map was too big to fit in your inventory."));
                        info(I.t("Use '/maptool getremaining' to get the remaining maps."));
                    }
                }

                @Override
                public void errored(Throwable exception) {
                    player.sendMessage(I.t("{ce}Map rendering failed: {0}", exception.getMessage()));

                    PluginLogger.warning("Rendering from {0} failed: {1}: {2}",
                            player.getName(),
                            exception.getClass().getCanonicalName(),
                            exception.getMessage());
                }
            });
        }
        //Added to fix bug with rendering displaying after error
        finally {
            ActionBar.removeMessage(player);
        }
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.NEW.grantedTo(sender);
    }
}
