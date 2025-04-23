package me.sandstorrm

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text

class RFCommand : ClientCommandRegistrationCallback {
    override fun register(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        registryAccess: CommandRegistryAccess
    ) {
        dispatcher.register(
            literal("rf")
                .executes {
                    RunefinderClient.ModState.toggle()
                    it.source.sendFeedback(Text.literal("Rune Finder toggled to: ${RunefinderClient.ModState.enabled}"))
                    1
                }
                .then(argument("query", greedyString()).executes { context ->
                    val lookingFor = context.getArgument("query", String::class.java)
                    RunefinderClient.lookingFor = lookingFor // set your variable
                    if (!RunefinderClient.ModState.enabled){
                        RunefinderClient.ModState.toggle()
                        context.source.sendFeedback(Text.literal("Rune Finder toggled to: ${RunefinderClient.ModState.enabled}"))
                    }
                    context.source.sendFeedback(Text.literal("Looking for: $lookingFor"))
                    1
                })
        )
    }
}