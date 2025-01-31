package de.sebastian;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ItemConfigHelper {

    public static List<Item> EXCLUDED_ENTRIES = new ArrayList<>();

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("eiiat_excluded_items.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveItems(List<Item> items) {
        List<String> itemIds = new ArrayList<>();
        for (Item item : items) {
            itemIds.add(Registries.ITEM.getId(item).toString());
        }

        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(itemIds, writer);
        } catch (IOException ignored) {
        }
    }

    public static List<Item> loadItems() {
        if (!CONFIG_PATH.toFile().exists()) return new ArrayList<>();

        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            List<String> itemIds = GSON.fromJson(reader, List.class);
            List<Item> items = new ArrayList<>();

            for (String id : itemIds) {
                Identifier identifier = Identifier.of(id);
                Item item = Registries.ITEM.get(identifier);
                if (item != null) {
                    items.add(item);
                }
            }

            return items;
        } catch (IOException ignored) {
        }

        return new ArrayList<>();
    }
}

