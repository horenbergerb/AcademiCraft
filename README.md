AcademiCraft
============

This is just a few modifications to ImageOnMap, an amazing bukkit plugin published by zDevelopers.
The goal is to provide features that make Minecraft a feasible medium for informal academic communication.

## Features

On top of the features of ImageOnMap, AcademiCraft lets you create LaTeX formulas in Minecraft.
The command "/latex <formula>" creates a map which can be used in the same way as the maps from ImageOnMap.

My next goal is to create a way to quickly convert powerpoints to Minecraft maps.

A future goal would be to create a way to convert whole .tex files into Minecraft, potentially using books.

## Requirements

This code was written somewhat particularly for my own computer. It assumes you are using Ubuntu.
Additionally, you must have the package "latex" installed at /usr/bin/latex

I also had to move /etc/ImageMagic-6/policy.xml to /etc/ImageMagic-6/HIDDEN_policy.xml
This is so that ImageMagic has necessary permissions to convert .tex files to .png.

When you type formulas in "/latex <formula>," assume they are already contained between two dollar signs in LaTeX.

## To Do

Probably lots of bug-fixing. I barely got this running the other day. I should do more testing.

I also want to get the "resize" command arguments working with the /latex command.

I could also afford to make the README better, rather than just writing on top of the old README.

ImageOnMap
==========

Repo for ImageOnMap, a bukkit plugin.


## Features

ImageOnMap allows you to load a picture from the Internet to a Minecraft map.

- Loads an image from a URL onto a map. PNG, JPEG and static GIF are supported.
- These images will be saved on your server and reloaded at restart.
- Big pictures will be cut automatically into several parts, to be rendered over multiple maps so they can cover whole
  walls! As example a 1024x1024 picture will be cut in 16 maps.
- Your image will be centered.
- You can put your map in an item frame, or in multiple ones at once—ImageOnMap handles the placement for you!

This plugin is a free software licenced under the [CeCILL-B licence](https://cecill.info/licences/Licence_CeCILL-B_V1-en.html)
(BSD-style in French law).


## Quick guide

- Ensure that you have a free slot in your inventory, as ImageOnMap will give you a map.
- Type `/tomap URL`, where URL is a link to the picture you want to render (see the section below).
- Enjoy your picture! You can place it in an item frame to make a nice poster if you want.


## Commands and Permissions

### `/tomap <url>`

Renders an image and gives a map to the player with it.

- This command can only be used by a player.
- The link must be complete, do not forget that the chat limit is 240 characters.
- You can use an URL shortener like tinyURL or bitly.
- If you want a picture in one map, type resize after the link.
- Permission: `imageonmap.new` (or `imageonmap.userender`—legacy, but will be kept in the plugin).


### `/maps`

Opens a GUI to see, retrieve and manage the user's maps.

- This command can only be used by a player.
- Opens a GUI listing all the maps in a pagnated view.
- A book is displayed too to see some usage statistics (maps created, quotas).
- An user can retrieve a map by left-clicking it, or manage it by right-clicking.
- Maps can be renamed (for organization), deleted (but they won't render in game anymore!), or partially retrieved (for posters maps containing more than one map).
- Permission: `imageonmap.list`, plus `imageonmap.get`, `imageonmap.rename` and `imageonmap.delete` for actions into the GUI.


### `/maptool <new|list|get|delete|explore|migrate>`

Main command to manage the maps. The less used in everyday usage, too.

- The commands names are pretty obvious.
- `/maptool new` is an alias of `/tomap`.
- `/maptool explore` is an alias of `/maps`.
- `/maptool migrate` migrates the old maps when you upgrade from IoM <= 2.7 to IoM 3.0. You HAVE TO execute this command to retrieve all maps when you do such a migration.
- Permissions:
  - `imageonmap.new` for `/maptool new`;
  - `imageonmap.list` for both `/maptool list` and `/maptool explore`;
  - `imageonmap.get` for `/maptool get`;
  - `imageonmap.delete` for `/maptool delete`;
  - `imageonmap.administrative` for `/maptool migrate`.

### About the permissions

All permissions are by default granted to everyone, with the exception of `imageonmap.administrative`. We believe that in most cases, servers administrators want to give the availability to create images on maps to every player.  
Negate a permission using a plugin manager to remove it, if you want to restrict this possibility to a set of users.

You can grant `imageonmap.*` to users, as this permission is a shortcut for all _user_ permissions (excluding `imageonmap.administrative`).


## Configuration

```yaml
# Plugin language. Empty: system language.
# Available: en-US (default, fallback), fr-FR, ru-RU, de-DE.
lang:

# Allows collection of anonymous statistics on plugin environment and usage
# The statistics are publicly visible here: http://mcstats.org/plugin/ImageOnMap
collect-data: true

# Images rendered on maps consume Minecraft maps ID, and there are only 32 767 of them.
# You can limit the maximum number of maps a player, or the whole server, can use with ImageOnMap.
# 0 means unlimited.
map-global-limit: 0
map-player-limit: 0

# Maximum size in pixels for an image to be. 0 is unlimited.
limit-map-size-x: 0
limit-map-size-y: 0

# Should the full image be saved when a map is rendered?
save-full-image: false
```

## Changelog

### 3.0 — The From-Scratch Update

The 3.0 release is a complete rewrite of the original ImageOnMap plugin, now based on QuartzLib, which adds many feature and fixes many bugs.

This new version is not compatible with the older ones, so your older maps will not be loaded. Run the `/maptool migrate` command (as op or in console) in order to get them back in this new version.

You will find amongst the new features:

- New Splatter maps, making it easy to deploy and remove big posters in one click !
- No more item tags when maps are put in item frames !
- Internationalization support (only french and english are supported for now, contributions are welcome)
- Map Quotas (for players and the whole server)
- A new map Manager (based on an inventory interface), to list, rename, get and delete your maps
- Improvements on the commands system (integrated help and autocompletion)
- Asynchronous maps rendering (your server won't freeze anymore when rendering big maps, and you can queue multiple map
  renderings !)
- UUID management (which requires to run `/maptool migrate`)

### 3.1 — The Permissions Update

- Fixed permissions support by adding a full set of permissions for every action of the plugin.

### 4.0 — Subtle Comfort

This version is a bit light in content, but we have unified part of the plugin (splatter map) and we prepared upcoming
changes with required zLib features. The next update should be bigger and will add more stuff : thumbnail, optimization,
possibility to deploy and place item frames in creative mode, creating interactive map that can run a command if you
click on a specific frame…
             
**This version is only compatible with Minecraft 1.15+.** Compatibility for 1.14 and below is dropped for now, but in
the future we will try to bring it back. Use 4.0 pre1 for now, if you require 1.13.2 – 1.14.4 compatibility. As for the
upcoming Minecraft 1.16 version, an update will add compatibility soon after its release.

- **You can now place a map on the ground or on a ceiling.**
- Languages with non-english characters now display correctly (fixed UTF-8 encoding bug).
- Splatter maps no longer throw an exception when placed.
- When a player place a splatter map, other players in the same area see it entirely, including the bottom-left corner.
- Added Russian and German translations (thx to Danechek and squeezer).


## Data collection

We use metrics to collect [basic information about the usage of this plugin](https://bstats.org/plugin/bukkit/ImageOnMap).
This is 100% anonymous (you can check the source code or the network traffic), but can of course be disabled by setting
`collect-data` to false in `config.yml`.
