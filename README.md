# TotalFreedomMod r03

[![Release](https://img.shields.io/github/languages/release/tfreedom/TotalFreedomMod?style=plastic)]
[![Version](https://img.shields.io/badge/version-1.21.10-blue?style=plastic)]
[![License](https://img.shields.io/github/languages/license/tfreedom/TotalFreedomMod?style=plastic)]
[![Code size](https://img.shields.io/github/languages/code-size/tfreedom/TotalFreedomMod?style=plastic)]

TotalFreedomMod is a CraftBukkit server plugin designed primarily to support the [TotalFreedom Minecraft Server](https://tfreedom.org/). However, you are more than welcome to adapt the source for your own server.

This plugin was originally coded by StevenLawson (Madgeek1450), with Jerom van der Sar (Prozza) becoming heavily involved in its development some time later. It consists of over 85 custom coded commands and a large variety of distinguishable features not included in any other plugin. The plugin has since its beginning grown immensely. Together, with the main TotalFreedom server, TotalFreedomMod has a long-standing reputation of effectiveness whilst maintaining a clear feeling of openness towards the administrators and the players themselves.

Since then, the plugin has been widely adopted by numerous servers in order to fully create the experience of a freedom server.  This repository seeks to bring that traditional server experience into the modern era by updating TotalFreedomMod to be compatible with newer versions of Minecraft servers.

As of current, TotalFreedomMod is compatible with Paper 1.21.10-R01 (last tested on 1.21.10-94-main@6794db4).  It may work on older versions and/or server types, however there has been no testing performed to this end.

### Hard dependencies
Only one plugin is required to run TotalFreedomMod, and that is *Aero*.  Aero is a lightweight commons library used in the construction of the plugin, and it is also open-source.
1. Aero 2.2+ (tested on aero-2.2-SNAPSHOT, [specially compiled](https://github.com/tfreedomorg/TotalFreedomMod/releases/tag/v6.0.0-pre1) for this plugin)

### Soft dependencies
The following plugins hook into TotalFreedomMod and are only necessary for specific features of the plugin to function as intended.  However, these plugins are not required for TotalFreedomMod to run.
1. [WorldEdit](https://github.com/EngineHub/WorldEdit) 7.3.x
2. [TF-WorldEdit](https://github.com/tfreedomorg/TF-WorldEdit) 2.0.x (tested on v2.0-pre1, [specially compiled](https://github.com/tfreedomorg/TF-WorldEdit/releases/tag/v2.0-pre1) for this plugin; requires WorldEdit)
3. [LibsDisguises](https://github.com/libraryaddict/LibsDisguises) 11.0.x
4. [EssentialsX] 2.21.x (including EssentialsXSpawn and other extensions)
5. [BukkitTelnet](https://github.com/TotalFreedom/BukkitTelnet) 4.x (deprecated; no longer guaranteed to work)

### Download
You may download official binaries from the [releases page](https://github.com/tfreedomorg/TotalFreedomMod/releases).

Additionally, you may compile TotalFreedomMod for yourself.  For more information, see [Compiling].

### Contributing
Please see [CONTRIBUTING.md](CONTRIBUTING.md) if you are interested in further developing TotalFreedomMod.

For information on how TotalFreedomMod is licensed, please see [LICENSE.md](LICENSE.md).

Please [join our Discord server](https://discord.com/invite/NXmyJwpwXr) for any discussion on TFM development and other TotalFreedom Organization projects.
