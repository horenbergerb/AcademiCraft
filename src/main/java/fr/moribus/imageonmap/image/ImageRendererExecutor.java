/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2020)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2020)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
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
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.moribus.imageonmap.image;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.worker.Worker;
import fr.zcraft.quartzlib.components.worker.WorkerAttributes;
import fr.zcraft.quartzlib.components.worker.WorkerCallback;
import fr.zcraft.quartzlib.components.worker.WorkerRunnable;
import fr.zcraft.quartzlib.tools.text.ActionBar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@WorkerAttributes(name = "Image Renderer", queriesMainThread = true)
public class ImageRendererExecutor extends Worker
{

    private static URLConnection connecting(URL url)throws IOException{
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        connection.connect();

        if (connection instanceof HttpURLConnection)
        {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            final int httpCode = httpConnection.getResponseCode();
            if ((httpCode / 100) != 2)
            {
                throw new IOException(I.t("HTTP error: {0} {1}", httpCode, httpConnection.getResponseMessage()));
            }
        }
        return connection;
    }

    static private void checkSizeLimit(final UUID playerUUID, final BufferedImage image) throws IOException {
        if ((PluginConfiguration.LIMIT_SIZE_X.get() > 0 || PluginConfiguration.LIMIT_SIZE_Y.get() > 0) && !Permissions.BYPASS_SIZE.grantedTo(Bukkit.getPlayer(playerUUID)))
        {
            if (PluginConfiguration.LIMIT_SIZE_X.get() > 0)
            {
                if (image.getWidth() > PluginConfiguration.LIMIT_SIZE_X.get())
                    throw new IOException(I.t("The image is too wide!"));
            }
            if (PluginConfiguration.LIMIT_SIZE_Y.get() > 0)
            {
                if (image.getHeight() > PluginConfiguration.LIMIT_SIZE_Y.get())
                    throw new IOException(I.t("The image is too tall!"));
            }
        }
    }

    private enum extension{
    png, jpg, jpeg, gif
    }

    static public void render(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID, final int width, final int height, WorkerCallback<ImageMap> callback)
    {
        submitQuery(new WorkerRunnable<ImageMap>()
        {
            @Override
            public ImageMap run() throws Throwable {

                BufferedImage image=null;
                //If the link is an imgur one
                if (url.toString().contains("https://imgur.com/")) {

                    //Not handled, can't with the hash only access the image in i.imgur.com/<hash>.<extension>


                    if (url.toString().contains("gallery/")) {
                        throw new IOException("We do not support imgur gallery yet, please use direct link to image instead. Right click on the picture you want to use then select copy picture link:) ");
                    }

                    for (extension ext : extension.values()) {
                        String newLink = "https://i.imgur.com/" + url.toString().split("https://imgur.com/")[1] + "." + ext.toString();
                        URL url2 = new URL(newLink);

                        //Try connecting
                        URLConnection connection = connecting(url2);

                        final InputStream stream = connection.getInputStream();

                        image = ImageIO.read(stream);

                        //valid image
                        if (image != null) break;

                    }


                }
                //If not an Imgur link
                else {
                    //Try connecting
                    URLConnection connection = connecting(url);

                    final InputStream stream = connection.getInputStream();

                    image = ImageIO.read(stream);
                }
                if (image == null) throw new IOException(I.t("The given URL is not a valid image"));
                // Limits are in place and the player does NOT have rights to avoid them.
                checkSizeLimit(playerUUID, image);
                if (scaling != ImageUtils.ScalingType.NONE && height <= 1 && width <= 1) {
                    return renderSingle(scaling.resize(image, ImageMap.WIDTH, ImageMap.HEIGHT), playerUUID);
                }
                final BufferedImage resizedImage = scaling.resize(image, ImageMap.WIDTH * width, ImageMap.HEIGHT * height);
                image.flush();
                return renderPoster(resizedImage, playerUUID);
            }
        }, callback);
    }

    static public void renderLatex(final String latexRaw, Player player, final ImageUtils.ScalingType scaling, final UUID playerUUID, final int width, final int height, WorkerCallback<ImageMap> callback)
    {
        submitQuery(new WorkerRunnable<ImageMap>()
        {
            @Override
            public ImageMap run() throws Throwable {

                BufferedImage image=null;

                //RENDER LATEX HERE
                try {
                    player.sendMessage("Creating tex file...");
                    //Creates a new tex file using the player's UUID as the name
                    File texFile = new File(ImageOnMap.getPlugin().getLatexDirectory(), playerUUID.toString()+".tex");
                    texFile.createNewFile();
                    //Writes the function to the tex file
                    FileWriter texWriter = new FileWriter(texFile,false);
                    player.sendMessage("Writing to tex file...");
                    texWriter.write(
                            "\\documentclass[convert={density=300,size=128x128,outext=.png}]{standalone}\n" +
                                    "\\nonstopmode\n" +
                                    "\n" +
                                    "\\usepackage{amsmath}\n" +
                                    "\\usepackage{amsthm}\n" +
                                    "\\usepackage{amsfonts}\n" +
                                    "\\usepackage{amssymb}\n" +
                                    "\\usepackage{tikz-cd}\n" +
                                    "\n" +
                                    "\\begin{document}\n" +
                                    "$" + latexRaw + "$\n" +
                            "\\end{document}"
                    );
                    texWriter.close();

                    player.sendMessage("Running png conversion...");
                    //Runs the shell code to generate the corresponding image
                    String[] args = new String[] {"/usr/bin/latex", "-interaction=nonstopmode", "--shell-escape", playerUUID.toString()+".tex"};
                    File stdout = new File(ImageOnMap.getPlugin().getLatexDirectory(), "stdout.txt");
                    Process proc = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(stdout).directory(ImageOnMap.getPlugin().getLatexDirectory()).start();
                    proc.waitFor();

                    player.sendMessage("Attempting to open image...");
                    //Opens the new image png
                    image = ImageIO.read(new File(ImageOnMap.getPlugin().getLatexDirectory(), playerUUID.toString()+".png"));

                }
                catch(Exception e) {
                    player.sendMessage("Something went wrong :(");
                    throw new IOException(I.t(e.getMessage()));
                }

                if (image == null) throw new IOException(I.t("New code produced empty image :("));
                // Limits are in place and the player does NOT have rights to avoid them.
                checkSizeLimit(playerUUID, image);
                if (scaling != ImageUtils.ScalingType.NONE && height <= 1 && width <= 1) {
                    return renderSingle(scaling.resize(image, ImageMap.WIDTH, ImageMap.HEIGHT), playerUUID);
                }
                final BufferedImage resizedImage = scaling.resize(image, ImageMap.WIDTH * width, ImageMap.HEIGHT * height);
                image.flush();
                return renderPoster(resizedImage, playerUUID);
            }
        }, callback);
    }

    public static void update(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID, final ImageMap map, final int width, final int height, WorkerCallback<ImageMap> callback) {
        submitQuery(new WorkerRunnable<ImageMap>()
        {
            @Override
            public ImageMap run() throws Throwable
            {

                final URLConnection connection = connecting(url);

                final InputStream stream = connection.getInputStream();
                final BufferedImage image = ImageIO.read(stream);
                stream.close();

                if (image == null) throw new IOException(I.t("The given URL is not a valid image"));

                // Limits are in place and the player does NOT have rights to avoid them.
                checkSizeLimit(playerUUID, image);

                updateMap(scaling.resize(image, width*128, height*128),playerUUID,map.getMapsIDs());
                return map;

            }
        }, callback);

    }
    static private void updateMap(final BufferedImage image, final UUID playerUUID,int[] mapsIDs) throws Throwable
    {

        final PosterImage poster = new PosterImage(image);
        poster.splitImages();

        ImageIOExecutor.saveImage(mapsIDs, poster);

        if (PluginConfiguration.SAVE_FULL_IMAGE.get())
        {
            ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), image);
        }

        submitToMainThread(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Renderer.installRenderer(poster, mapsIDs);
                return null;
            }
        });
    }

    static private ImageMap renderSingle(final BufferedImage image, final UUID playerUUID) throws Throwable
    {
        MapManager.checkMapLimit(1, playerUUID);
        final Future<Integer> futureMapID = submitToMainThread(new Callable<Integer>()
        {
            @Override
            public Integer call() throws Exception
            {
                return MapManager.getNewMapsIds(1)[0];
            }
        });

        final int mapID = futureMapID.get();
        ImageIOExecutor.saveImage(mapID, image);

        submitToMainThread(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Renderer.installRenderer(image, mapID);
                image.flush();
                return null;
            }
        });
        image.flush();
        return MapManager.createMap(playerUUID, mapID);
    }

    static private ImageMap renderPoster(final BufferedImage image, final UUID playerUUID) throws Throwable
    {
        final PosterImage poster = new PosterImage(image);
        final int mapCount = poster.getImagesCount();
        MapManager.checkMapLimit(mapCount, playerUUID);
        final Future<int[]> futureMapsIds = submitToMainThread(new Callable<int[]>()
        {
            @Override
            public int[] call() throws Exception
            {
                return MapManager.getNewMapsIds(mapCount);
            }
        });
        poster.splitImages();
        final int[] mapsIDs = futureMapsIds.get();
        ImageIOExecutor.saveImage(mapsIDs, poster);


        if (PluginConfiguration.SAVE_FULL_IMAGE.get())
        {
            ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), image);
        }

        submitToMainThread(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Renderer.installRenderer(poster, mapsIDs);
                return null;
            }

        });

        image.flush();

        return MapManager.createMap(poster, playerUUID, mapsIDs);
    }


}
