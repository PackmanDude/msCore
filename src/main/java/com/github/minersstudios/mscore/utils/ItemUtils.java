package com.github.minersstudios.mscore.utils;

import com.github.minersstudios.msitems.items.DamageableItem;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public final class ItemUtils {

	@Contract(value = " -> fail")
	private ItemUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets an {@link ItemStack} of custom item/block/decor
	 *
	 * @param namespacedKeyStr namespaced key string, example - (msitem:example)
	 * @return {@link ItemStack} of custom item/block/decor
	 * @throws NullPointerException if it can't find custom item/block/decor with this namespaced key
	 */
	@Contract("null -> null")
	public static @Nullable ItemStack getMSItemStack(@Nullable String namespacedKeyStr) {
		if (namespacedKeyStr == null) return null;
		if (namespacedKeyStr.matches("msitem:\\w+")) {
			return MSItemUtils.getCustomItemItemStack(namespacedKeyStr);
		} else if (namespacedKeyStr.matches("msblock:\\w+")) {
			return MSBlockUtils.getCustomBlockItem(namespacedKeyStr);
		} else if (namespacedKeyStr.matches("msdecor:\\w+")) {
			return MSDecorUtils.getCustomDecorItem(namespacedKeyStr);
		}
		return null;
	}

	@Contract("null, null -> false")
	public static boolean isSimilarItemStacks(@Nullable ItemStack first, @Nullable ItemStack second) {
		if (
				first == null
				|| second == null
				|| first.getType() != second.getType()
		) return false;
		ItemMeta firstMeta = first.getItemMeta();
		ItemMeta secondMeta = second.getItemMeta();
		if (
				!firstMeta.hasCustomModelData()
				|| !secondMeta.hasCustomModelData()
		) return false;
		return firstMeta.getCustomModelData() == secondMeta.getCustomModelData();
	}

	@Contract("_, null -> false")
	public static boolean isListContainsItem(@NotNull List<ItemStack> list, @Nullable ItemStack item) {
		if (list.isEmpty() || item == null) return false;
		for (ItemStack listItem : list) {
			if (isSimilarItemStacks(listItem, item)) return true;
		}
		return false;
	}

	public static boolean damageItem(@NotNull Player holder, @Nullable ItemStack item) {
		return damageItem(holder, item, 1);
	}

	public static boolean damageItem(@NotNull Player holder, @Nullable ItemStack item, int damage) {
		return damageItem(holder, EquipmentSlot.HAND, item, damage);
	}

	public static boolean damageItem(@NotNull Player holder, @Nullable EquipmentSlot slot, @Nullable ItemStack item, int originalDamage) {
		if (item == null) return false;
		if (item.getItemMeta() instanceof Damageable damageable) {
			int damage = 0;

			DamageableItem damageableItem = DamageableItem.fromItemStack(item);
			if (damageableItem != null) {
				damageableItem.setRealDamage(damageableItem.getRealDamage() + originalDamage);
				if (
						!damageable.hasEnchant(Enchantment.DURABILITY)
						|| Math.random() < 1.0d / (damageable.getEnchantLevel(Enchantment.DURABILITY) + 1.0d)
				) {
					damage = originalDamage;
					damageableItem.saveForItemStack(item);
				}
			} else if (
					!damageable.hasEnchant(Enchantment.DURABILITY)
					|| Math.random() < 1.0d / (damageable.getEnchantLevel(Enchantment.DURABILITY) + 1.0d)
			) {
				damage = originalDamage;
				damageable.setDamage(damageable.getDamage() + originalDamage);
				item.setItemMeta(damageable);
			}

			if (damageableItem == null) {
				PlayerItemDamageEvent event = new PlayerItemDamageEvent(holder, item, damage, originalDamage);
				Bukkit.getPluginManager().callEvent(event);
			}

			if (
					damageableItem != null
					? damageableItem.getRealDamage() >= damageableItem.getMaxDamage()
					: damageable.getDamage() >= item.getType().getMaxDurability()
			) {
				item.setAmount(item.getAmount() - 1);

				if (item.getType() == Material.SHIELD) {
					holder.playEffect(EntityEffect.SHIELD_BREAK);
					return true;
				}

				switch (slot == null ? EquipmentSlot.HAND : slot) {
					case HEAD -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_HELMET);
					case CHEST -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_CHESTPLATE);
					case LEGS -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_LEGGINGS);
					case FEET -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_BOOTS);
					case OFF_HAND -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_OFF_HAND);
					default -> holder.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND);
				}

				holder.updateInventory();
			}
			return true;
		}
		return false;
	}
}
