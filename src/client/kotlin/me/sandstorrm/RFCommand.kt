package me.sandstorrm

import com.mojang.brigadier.CommandDispatcher
import me.sandstorrm.commands.Array
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text
import me.sandstorrm.commands.Find
import me.sandstorrm.commands.Preset
import me.sandstorrm.commands.RestrictToArrays


class RFCommand : ClientCommandRegistrationCallback {

    override fun register(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        registryAccess: CommandRegistryAccess
    ) {
        dispatcher.register(
            literal("rf")
                .executes { ctx ->
                    RunefinderClient.ModState.toggle()
                    ctx.source.sendFeedback(Text.literal("Rune Finder toggled to: ${RunefinderClient.ModState.enabled}"))
                    1
                }
                .then(Find.build())
                .then(Array.build())
                .then(Preset.build())
                .then(RestrictToArrays.build())
        )
    }
}
