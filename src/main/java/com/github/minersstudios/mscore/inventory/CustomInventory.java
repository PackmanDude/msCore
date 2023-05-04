package com.github.minersstudios.mscore.inventory;

import com.github.minersstudios.mscore.inventory.actions.BottomInventoryClickAction;
import com.github.minersstudios.mscore.inventory.actions.InventoryClickAction;
import com.github.minersstudios.mscore.inventory.actions.InventoryCloseAction;
import com.github.minersstudios.mscore.inventory.actions.InventoryOpenAction;
import com.github.minersstudios.mscore.utils.ChatUtils;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.Map;

public class CustomInventory extends CraftInventoryCustom {
	private final int size;
	private final @NotNull Map<Integer, InventoryButton> buttons;
	private @Nullable InventoryOpenAction openAction;
	private @Nullable InventoryCloseAction closeAction;
	private @Nullable InventoryClickAction clickAction;
	private @Nullable BottomInventoryClickAction bottomClickAction;
	private Object[] args;

	private static final int MAX_SIZE = 54;

	public CustomInventory(
			@NotNull String title,
			@Range(from = 1, to = 6) int verticalSize,
			Object... args
	) {
		super(null, verticalSize * 9, ChatUtils.createDefaultStyledText(title));
		this.size = verticalSize * 9;
		this.buttons = new HashMap<>(this.size);
		this.args = args;
	}

	public @NotNull Map<Integer, InventoryButton> getButtons() {
		return this.buttons;
	}

	public boolean hasButtons() {
		return !this.buttons.isEmpty();
	}

	public void setButtons(@NotNull Map<Integer, InventoryButton> buttons) {
		for (Map.Entry<Integer, InventoryButton> entry : buttons.entrySet()) {
			this.setButtonAt(entry.getKey(), entry.getValue());
		}
		this.updateButtons();
	}

	public boolean setButtonAt(
			@Range(from = 0, to = MAX_SIZE) int slot,
			@NotNull InventoryButton button
	) {
		if (slot + 1 > this.size) return false;
		this.buttons.put(slot, button);
		return true;
	}

	public boolean setButtonAndUpdate(
			@Range(from = 0, to = MAX_SIZE) int slot,
			@NotNull InventoryButton button
	) {
		if (this.setButtonAt(slot, button)) return false;
		this.updateButtons();
		return true;
	}

	public boolean removeButtonAt(@Range(from = 0, to = MAX_SIZE) int slot) {
		return slot + 1 <= this.size && this.buttons.remove(slot) != null;
	}

	public @Nullable InventoryButton getClickedButton(int slot) {
		return this.buttons.getOrDefault(slot, null);
	}

	public void updateButtons() {
		if (!this.hasButtons()) return;
		for (Map.Entry<Integer, InventoryButton> entry : this.buttons.entrySet()) {
			this.setItem(entry.getKey(), entry.getValue().getItem());
		}
	}

	public @Nullable InventoryOpenAction getOpenAction() {
		return this.openAction;
	}

	public void setOpenAction(@Nullable InventoryOpenAction openAction) {
		this.openAction = openAction;
	}

	public void doOpenAction(@NotNull Player player) {
		if (this.openAction != null) {
			this.openAction.doAction(player, this);
		}
	}

	public @Nullable InventoryCloseAction getCloseAction() {
		return this.closeAction;
	}

	public void setCloseAction(@Nullable InventoryCloseAction closeAction) {
		this.closeAction = closeAction;
	}

	public void doCloseAction(@NotNull Player player) {
		if (this.closeAction != null) {
			this.closeAction.doAction(player, this);
		}
	}

	public @Nullable InventoryClickAction getClickAction() {
		return this.clickAction;
	}

	public void setClickAction(@Nullable InventoryClickAction clickAction) {
		this.clickAction = clickAction;
	}

	public void doClickAction(
			@NotNull Player player,
			@NotNull InventoryAction action,
			@NotNull ClickType clickType,
			@Nullable ItemStack currentItem,
			@Nullable ItemStack cursorItem
	) {
		if (this.clickAction != null) {
			this.clickAction.doAction(player, this, action, clickType, currentItem, cursorItem);
		}
	}

	public @Nullable BottomInventoryClickAction getBottomInventoryClickAction() {
		return this.bottomClickAction;
	}

	public void setBottomInventoryClickAction(@Nullable BottomInventoryClickAction bottomInventoryClickAction) {
		this.bottomClickAction = bottomInventoryClickAction;
	}

	public void doBottomClickAction(
			@NotNull Player player,
			@NotNull InventoryAction action,
			@NotNull ClickType clickType,
			@Nullable ItemStack currentItem
	) {
		if (this.bottomClickAction != null) {
			this.bottomClickAction.doAction(player, this, action, clickType, currentItem);
		}
	}

	public Object[] getArgs() {
		return this.args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}
}
