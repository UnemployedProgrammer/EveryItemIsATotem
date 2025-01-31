package de.sebastian;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// getString(ctx, "string")
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
// word()
import static com.mojang.brigadier.arguments.StringArgumentType.word;
// literal("foo")
import static net.minecraft.server.command.CommandManager.literal;
// argument("bar", word())
import static net.minecraft.server.command.CommandManager.argument;
// Import everything in the CommandManager
import static net.minecraft.server.command.CommandManager.*;

public class EIIAT implements ModInitializer {
	public static final String MOD_ID = "eiiat";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing EIIAT (Every Item Is A Totem)...");

		ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> {
			for (ServerPlayerEntity serverPlayerEntity : minecraftServer.getPlayerManager().getPlayerList()) {
				for (int i = 0; i < serverPlayerEntity.getInventory().size(); i++) { // Iterate over slots
					ItemStack stack = serverPlayerEntity.getInventory().getStack(i);
					if (!stack.isEmpty() && !ItemConfigHelper.EXCLUDED_ENTRIES.contains(stack.getItem())) {
						stack.set(DataComponentTypes.DEATH_PROTECTION, DeathProtectionComponent.TOTEM_OF_UNDYING);
					}
				}
			}
		});

		ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
			ItemConfigHelper.saveItems(ItemConfigHelper.EXCLUDED_ENTRIES);
		});

		ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
			ItemConfigHelper.EXCLUDED_ENTRIES = ItemConfigHelper.loadItems();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess, registrationEnvironment) -> {
			dispatcher.register(
					literal("eiiat").requires(source -> source.hasPermissionLevel(4))
							.then(
									literal("exclude")
											.then(argument("item", ItemStackArgumentType.itemStack(commandRegistryAccess))
													.executes(commandContext -> {
														Item item = ItemStackArgumentType.getItemStackArgument(commandContext, "item").getItem();
														if(ItemConfigHelper.EXCLUDED_ENTRIES.contains(item)) {
															commandContext.getSource().sendFeedback(() -> format(Formatting.RED, "blacklisting.eiiat.fail", Text.translatable(item.getTranslationKey()).getString()), false);
															return 1;
														}
														ItemConfigHelper.EXCLUDED_ENTRIES.add(item);
														commandContext.getSource().sendFeedback(() -> format(Formatting.GREEN, "blacklisting.eiiat.success", Text.translatable(item.getTranslationKey()).getString()), true);
														return 1;
													})
											)
							)
							.then(
									literal("include")
											.then(argument("item", ItemStackArgumentType.itemStack(commandRegistryAccess))
													.executes(commandContext -> {
														Item item = ItemStackArgumentType.getItemStackArgument(commandContext, "item").getItem();
														if(!ItemConfigHelper.EXCLUDED_ENTRIES.contains(item)) {
															commandContext.getSource().sendFeedback(() -> format(Formatting.RED, "including.eiiat.fail", Text.translatable(item.getTranslationKey()).getString()), true);
															return 1;
														}
														ItemConfigHelper.EXCLUDED_ENTRIES.remove(item);
														commandContext.getSource().sendFeedback(() -> format(Formatting.GREEN, "including.eiiat.success", Text.translatable(item.getTranslationKey()).getString()), true);
														return 1;
													})
											)
							)
							.then(
									literal("get")
											.executes(commandContext -> {
												commandContext.getSource().sendFeedback(() -> Text.translatable("get.eiiat.are"), false);
												for (Item excludedEntry : ItemConfigHelper.EXCLUDED_ENTRIES) {
													commandContext.getSource().sendFeedback(() -> formatAndExecuteCommand(Formatting.BLUE, "get.eiiat.item", Text.translatable(excludedEntry.getTranslationKey()).getString(), "get.eiiat.click", "/eiiat exclude " + Registries.ITEM.getId(excludedEntry)), false);
												}
												return 1;
											})
							)
			);
		});
	}

	public static Text format(Formatting formatting, String key, String replacer) {
		return Text.literal(Text.translatable(key).getString().replace("{}", replacer)).formatted(formatting);
	}

	public static Text formatAndExecuteCommand(Formatting formatting, String key, String replacer, String hoverKey, String clickCommand) {
		return Text.literal(Text.translatable(key).getString().replace("{}", replacer)).setStyle(Style.EMPTY.withColor(formatting).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable(hoverKey))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand)));
	}
}