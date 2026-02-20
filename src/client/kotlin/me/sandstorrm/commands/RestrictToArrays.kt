package me.sandstorrm.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.shedaniel.autoconfig.AutoConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

object RestrictToArrays {

    private fun holder() =
        AutoConfig.getConfigHolder(me.sandstorrm.ModConfig::class.java)

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("restrictarrays")

            // /rf restrictarrays
            .executes { ctx ->

                val h = holder()
                val cfg = h.config

                cfg.restrictToArrays = !cfg.restrictToArrays

                h.save()

                ctx.source.sendFeedback(
                    Text.literal(
                        "Restrict to arrays: ${if (cfg.restrictToArrays) "ON" else "OFF"}"
                    )
                )

                1
            }

            // /rf restrictarrays on
            .then(
                literal("on")
                    .executes { ctx ->

                        val h = holder()
                        val cfg = h.config

                        cfg.restrictToArrays = true
                        h.save()

                        ctx.source.sendFeedback(
                            Text.literal("Restrict to arrays: ON")
                        )

                        1
                    }
            )

            // /rf restrictarrays off
            .then(
                literal("off")
                    .executes { ctx ->

                        val h = holder()
                        val cfg = h.config

                        cfg.restrictToArrays = false
                        h.save()

                        ctx.source.sendFeedback(
                            Text.literal("Restrict to arrays: OFF")
                        )

                        1
                    }
            )
}
