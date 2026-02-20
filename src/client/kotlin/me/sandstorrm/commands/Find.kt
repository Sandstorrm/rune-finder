package me.sandstorrm.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import me.sandstorrm.RunefinderClient
import me.shedaniel.autoconfig.AutoConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import java.util.concurrent.CompletableFuture

object Find {

    private fun holder() = AutoConfig.getConfigHolder(me.sandstorrm.ModConfig::class.java)

    private val PRESET_SUGGEST: SuggestionProvider<FabricClientCommandSource> =
        SuggestionProvider { _, builder ->
            val cfg = holder().config
            val presetName = cfg.activePreset
            if (presetName.isBlank()) {
                // preset cleared => no autocomplete
                return@SuggestionProvider CompletableFuture.completedFuture(builder.build())
            }

            val items = cfg.presets[presetName]?.items.orEmpty()
            for (s in items) builder.suggest(s)

            CompletableFuture.completedFuture(builder.build())
        }

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("find")
            .then(
                argument("query", StringArgumentType.greedyString())
                    .suggests(PRESET_SUGGEST)
                    .executes { ctx ->
                        val q = StringArgumentType.getString(ctx, "query")
                        RunefinderClient.lookingFor = q

                        if (!RunefinderClient.ModState.enabled) {
                            RunefinderClient.ModState.toggle()
                            ctx.source.sendFeedback(
                                Text.literal("Rune Finder toggled to: ${RunefinderClient.ModState.enabled}")
                            )
                        }

                        ctx.source.sendFeedback(Text.literal("Looking for: $q"))
                        1
                    }
            )
}
