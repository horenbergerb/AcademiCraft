/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap.commands;

import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.zcraft.quartzlib.components.commands.Command;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.i18n.I;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;


public abstract class IoMCommand extends Command {

    protected boolean checkHostnameWhitelist(final URL url) {
        final List<String> hostnames = PluginConfiguration.IMAGES_HOSTNAMES_WHITELIST.get()
                .stream()
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toList());

        if (hostnames.isEmpty()) {
            return true;
        }

        return hostnames
                .stream()
                .map(h -> h.replaceAll("https://", "").replaceAll("http://", ""))
                .anyMatch(h -> h.equalsIgnoreCase(url.getHost()));
    }

    protected void retrieveUUID(final String arg, final Consumer<UUID> consumer) {
        // If it is being removed we may have to use Mojang services
        consumer.accept(Bukkit.getOfflinePlayer(arg).getUniqueId());
    }

    protected ImageMap getMapFromArgs() throws CommandException {
        return getMapFromArgs(playerSender(), 0, true);
    }

    protected ImageMap getMapFromArgs(final Player player, final int index, boolean expand) throws CommandException {
        if (args.length <= index) {
            throwInvalidArgument(I.t("You need to give a map name."));
        }

        ImageMap map;
        String mapName = args[index];

        if (expand) {
            for (int i = index + 1, c = args.length; i < c; i++) {
                mapName += " " + args[i];
            }
        }

        mapName = mapName.trim();
        map = MapManager.getMap(player.getUniqueId(), mapName);

        if (map == null) {
            error(I.t("This map does not exist."));
        }

        return map;
    }

    protected ArrayList<String> getArgs() {
        ArrayList<String> arguments = new ArrayList<>();

        //State of the automaton, can read word like:
        //name_here; "name here"
        int state = 0;
        StringBuilder s = new StringBuilder();

        for (String arg : args) {
            if (arg.startsWith("http:") || arg.startsWith("https:")) {
                arguments.add(arg);
                continue;
            }
            if (state == 0) {
                s = new StringBuilder();
            } else {
                s.append(" ");
            }
            for (char c : arg.toCharArray()) {
                switch (state) {
                    case 0:
                        if (c == '\"') {
                            state = 1;
                        } else {
                            //If we read a : that means that we are on a new argument example:"hello"
                            if (c == ':') {
                                arguments.add(s.toString());
                                s = new StringBuilder();
                            } else {
                                s = s.append(c);
                            }
                        }
                        break;
                    case 1:
                        if (c == '\"') {
                            arguments.add(s.toString());
                            s = new StringBuilder();
                            state = 0;
                        } else {
                            s = s.append(c);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + state);
                }
            }
            if (s.length() > 0 && state != 1) {
                arguments.add(s.toString());
            }

        }
        return arguments;
    }


    protected List<String> getMatchingMapNames(Player player, String prefix) {
        return getMatchingMapNames(MapManager.getMapList(player.getUniqueId()), prefix);
    }

    protected List<String> getMatchingMapNames(Iterable<? extends ImageMap> maps, String prefix) {
        List<String> matches = new ArrayList<>();

        for (ImageMap map : maps) {
            if (map.getId().startsWith(prefix)) {
                matches.add(map.getId());
            }
        }

        return matches;
    }
}
