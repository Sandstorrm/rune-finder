package me.sandstorrm.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import me.shedaniel.autoconfig.AutoConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import java.util.concurrent.CompletableFuture

object Preset {

    private fun holder() =
        AutoConfig.getConfigHolder(me.sandstorrm.ModConfig::class.java)

    private val PRESET_NAME_SUGGEST: SuggestionProvider<FabricClientCommandSource> =
        SuggestionProvider { _, builder ->
            val cfg = holder().config
            cfg.presets.keys.sorted().forEach { builder.suggest(it) }
            CompletableFuture.completedFuture(builder.build())
        }

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("preset")

            // /rf preset clear
            .then(
                literal("clear")
                    .executes { ctx ->
                        val h = holder()
                        val cfg = h.config
                        cfg.activePreset = ""
                        h.save()

                        ctx.source.sendFeedback(
                            Text.literal("Preset cleared. /rf find will not autocomplete.")
                        )
                        1
                    }
            )

            // /rf preset <preset_name>
            .then(
                argument("preset_name", StringArgumentType.word())
                    .suggests(PRESET_NAME_SUGGEST)
                    .executes { ctx ->
                        val name = StringArgumentType.getString(ctx, "preset_name")

                        val h = holder()
                        val cfg = h.config

                        cfg.activePreset = name
                        h.save()

                        val count = cfg.presets[name]?.items?.size ?: 0

                        ctx.source.sendFeedback(
                            Text.literal("Active preset set to '$name' ($count entries).")
                        )
                        1
                    }
            )
}
