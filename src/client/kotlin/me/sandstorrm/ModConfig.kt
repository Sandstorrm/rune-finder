package me.sandstorrm

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config

@Config(name = RunefinderClient.MODID)
class ModConfig : ConfigData {
    var delayBetweenDrops: Int = 5
    var delayBeforeStartDrop: Int = 1
    var delayAfrerChestClear: Int = 0
    var restrictToArrays: Boolean = true

    // name -> stored box
    var arrays: MutableMap<String, SavedArray> = mutableMapOf()

    // which preset is currently active ("", means none)
    var activePreset: String = ""

    // preset name -> list of strings (items/runes/etc)
    var presets: MutableMap<String, Preset> = mutableMapOf()


}

class Preset {
    var items: MutableList<String> = mutableListOf()
}

class SavedArray {
    var ax: Int = 0; var ay: Int = 0; var az: Int = 0
    var bx: Int = 0; var by: Int = 0; var bz: Int = 0
}
