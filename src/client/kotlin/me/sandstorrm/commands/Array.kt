package me.sandstorrm.commands

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import me.sandstorrm.SavedArray
import me.shedaniel.autoconfig.AutoConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture

/**
 * Client-only BlockPos argument that parses: <x> <y> <z>
 * This avoids ServerCommandSource-only BlockPosArgumentType.
 */
class ClientBlockPosArgumentType : ArgumentType<BlockPos> {
    override fun parse(reader: StringReader): BlockPos {
        val x = reader.readInt()
        reader.skipWhitespace()
        val y = reader.readInt()
        reader.skipWhitespace()
        val z = reader.readInt()
        return BlockPos(x, y, z)
    }

    companion object {
        fun blockPos(): ClientBlockPosArgumentType = ClientBlockPosArgumentType()
    }
}

object Array {

    private fun holder() = AutoConfig.getConfigHolder(me.sandstorrm.ModConfig::class.java)

    private fun lookedPos(): BlockPos? {
        val client = MinecraftClient.getInstance()
        val lookedAt = (client.crosshairTarget as? BlockHitResult)?.blockPos
        val fallback = client.player?.blockPos
        return lookedAt ?: fallback
    }

    // Suggest "x y z" for the block you're looking at (fallback: your feet)
    private val LOOK_POS_TRIPLE_SUGGEST: SuggestionProvider<FabricClientCommandSource> =
        SuggestionProvider { _, builder ->
            val pos = lookedPos()
            if (pos != null) builder.suggest("${pos.x} ${pos.y} ${pos.z}")
            CompletableFuture.completedFuture(builder.build())
        }

    // Suggest saved array names for remove/show/etc
    private val ARRAY_NAME_SUGGEST: SuggestionProvider<FabricClientCommandSource> =
        SuggestionProvider { _, builder ->
            val cfg = holder().config
            cfg.arrays.keys.sorted().forEach { builder.suggest(it) }
            CompletableFuture.completedFuture(builder.build())
        }

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("array")

            // /rf array list
            .then(
                literal("list")
                    .executes { ctx ->
                        val cfg = holder().config
                        if (cfg.arrays.isEmpty()) {
                            ctx.source.sendFeedback(Text.literal("No arrays saved."))
                            return@executes 1
                        }

                        // Keep it readable in chat: one per line
                        val lines = cfg.arrays.entries
                            .sortedBy { it.key }
                            .joinToString("\n") { (name, a) ->
                                "$name: A(${a.ax},${a.ay},${a.az}) B(${a.bx},${a.by},${a.bz})"
                            }

                        ctx.source.sendFeedback(Text.literal(lines))
                        1
                    }
            )

            // /rf array add <name> <x y z> <x y z>
            .then(
                literal("add")
                    .then(
                        argument("name", StringArgumentType.word())
                            .then(
                                argument("posA", ClientBlockPosArgumentType.blockPos()).suggests(LOOK_POS_TRIPLE_SUGGEST)
                                    .then(
                                        argument("posB", ClientBlockPosArgumentType.blockPos()).suggests(LOOK_POS_TRIPLE_SUGGEST)
                                            .executes { ctx ->
                                                val name = StringArgumentType.getString(ctx, "name")
                                                val a = ctx.getArgument("posA", BlockPos::class.java)
                                                val b = ctx.getArgument("posB", BlockPos::class.java)

                                                val h = holder()
                                                val cfg = h.config

                                                cfg.arrays[name] = SavedArray().apply {
                                                    ax = a.x; ay = a.y; az = a.z
                                                    bx = b.x; by = b.y; bz = b.z
                                                }

                                                h.save()

                                                ctx.source.sendFeedback(
                                                    Text.literal("Saved array '$name' A(${a.x},${a.y},${a.z}) B(${b.x},${b.y},${b.z})")
                                                )
                                                1
                                            }
                                    )
                            )
                    )
            )

            // /rf array remove <name>
            .then(
                literal("remove")
                    .then(
                        argument("name", StringArgumentType.word())
                            .suggests(ARRAY_NAME_SUGGEST)
                            .executes { ctx ->
                                val name = StringArgumentType.getString(ctx, "name")

                                val h = holder()
                                val cfg = h.config

                                val removed = cfg.arrays.remove(name) != null
                                if (removed) h.save()

                                ctx.source.sendFeedback(
                                    Text.literal(if (removed) "Removed array '$name'" else "No array named '$name'")
                                )
                                1
                            }
                    )
            )
}
