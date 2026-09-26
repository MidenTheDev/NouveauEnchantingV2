# Nouveau Enchanting
![GitHub Latest Stable Release](https://img.shields.io/github/v/release/MidenTheDev/NouveauEnchantingV2) ![GitHub Latest Release](https://img.shields.io/github/v/release/MidenTheDev/NouveauEnchantingV2?include_prereleases)
![Static Badge](https://img.shields.io/badge/Requires%20Version%2026.1.2%2B-00BF3F?style=flat) ![Static Badge](https://img.shields.io/badge/Requires%20Paper-BF5600?style=flat&link=https%3A%2F%2Fpapermc.io%2Fdownloads%2Fpaper)

A complete, ground-up overhaul of Minecraft's Enchanting system inspired by Tinkers' Construct for Paper Minecraft Servers


## :crystal_ball: Material-Based Enchanting:
Enchant your tools, weapons, and armor incrementally by using materials and exp instead of farming for villagers or gambling for the right enchants in the enchanting table.

For example, to add Sharpness 1 to a sword, you go to the enchanting table with some nether quartz. The table will take 4 levels and 8 quartz to apply Sharpness 1. Then, to apply Sharpness 2, take that sword with Sharpness 1, put it back in the table with some more quartz, and it will take 6 levels and 4 quartz and apply Sharpness 2, and so on and so forth.

This all comes complete with a GUI that (optionally) replaces the vanilla Enchanting Table with an item previewing the level and material cost of every enchant you try to apply. With a default configuration, it looks like this:

![](https://github.com/MidenTheDev/NouveauEnchantingV2/blob/master/page_resources/enchanting_demo.gif)

Everything in the plugin is configurable via the materials.yml file and you can make any material correspond to any enchantment. You can even use custom materials added by other plugins! Here is a list of common plugins and whether or not Nouveau Enchanting supports them:

| Plugin |Has Support   |
| ------------ | ------------ |
|ItemsAdder  | :white_check_mark:  |
|ServerLevels| :white_check_mark: |
| Oraxen  | :x:  |
|CraftEngine| :x: (planned)|

Please note: **changes to your materials.yml file require a full server restart to apply!**
## :sparkles: Vanilla Enchant Overrides:
Using Paper's new Registry API, this plugin allows you to directly modify the vanilla game's Enchants. Currently you can:
- Change what items Enchants can be applied to
- Change the max level of vanilla Enchants

All of this is done in the plugin's enchants.yml file, which comes pre-configured with every enchant in the game set to their vanilla values. **Updates to this file require a full server restart!**

## :computer: Commands:
**/ne reload** - Reloads the config.yml. Does **NOT** reload enchants.yml or materials.yml. Changes to these files require a full server restart.

**/ne opengui** - Opens the Enchanting GUI for yourself

**/ne openguiOther [player]** - Opens the Enchanting GUI for the specified player

## :watch: Planned Features:
Eventually, I would like for this plugin to:
- Modify anvil behavior to block the application of enchanted books
- Modify Villager trades to remove enchanted books and enchanted tools (Likely as a separate plugin)
- Allow the user to modify the functionality of vanilla enchants (Such as changing how much damage sharpness adds or how much Protection enchants protect you)
- Have a dedicated API or other system for creating custom enchantments that natively use the new system.

None of the above features are currently implemented in the plugin, but are planned for future updates.

## :question: FAQ:

#### Q:  I Need Help! X Isn't working!
**A: **Check the relevant Wiki page. If the Wiki isn't helpful or you otherwise don't understand, join the Discord Server and ask for support there. (Discord W.I.P)

#### Q: Why does this plugin only support Paper and not Spigot?
**A:** This plugin makes extensive use of Paper's api, even using features that are still considered experiemental and not officially supported. To make a plugin that achieves the same result using Spigot, while possible, would be a massive undertaking and would be a lot laggier than this plugin is currently on Paper.

#### Q: Why does the Github repo say V2?
**A:** This plugin is actually the 2nd iteration of the plugin that I've made. The original V1 version was made with the Spigot API (rather than Paper) and had half the feautres with twice the lag. V1 never saw release since I was never fully satisfied with my work or the overall quality of my code.
