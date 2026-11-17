package de.spearmace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Speer in der Hand + Spieler faellt (in der Luft mit negativer Y-Geschwindigkeit) + Linksklick
 * -> automatisch auf den Mace wechseln.
 *
 * Abgestimmt auf die Mod "windspear" (Wind Charge -> Speer):
 * Gewechselt wird nur, wenn der Speer schon am ENDE des vorherigen Ticks in der Hand war
 * UND der Angriffsklick in diesem Tick neu beginnt. So wechselt ein einzelner Klick nie in
 * einem Zug von der Wind Charge ueber den Speer zum Mace: Klick 1 = Speer, Klick 2 = Mace.
 * Die Logik haengt nicht von der Reihenfolge der Mods ab.
 *
 * Fallstrecke und Schwung gehoeren zum Spieler, nicht zur Waffe, und bleiben beim
 * Slot-Wechsel deshalb automatisch erhalten.
 */
public class SpearMaceClient implements ClientModInitializer {

    private boolean attackWasDown;
    private boolean spearHeldAtEndOfLastTick;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(this::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onStartTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            attackWasDown = false;
            return;
        }

        boolean down = client.options.keyAttack.isDown();
        boolean newClick = down && !attackWasDown;
        attackWasDown = down;

        if (!newClick) {
            return;
        }
        if (!spearHeldAtEndOfLastTick || !isSpear(client.player.getMainHandItem())) {
            return;
        }
        if (!isFalling(client.player)) {
            return;
        }

        Inventory inv = client.player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inv.getItem(slot).is(Items.MACE)) {
                inv.setSelectedSlot(slot);
                return;
            }
        }
    }

    private void onEndTick(Minecraft client) {
        spearHeldAtEndOfLastTick = client.player != null && isSpear(client.player.getMainHandItem());
    }

    /**
     * Prueft, ob der Spieler in der Luft ist UND nach unten faellt.
     * onGround == false UND negative Y-Geschwindigkeit (DeltaMovement.y < 0).
     */
    private static boolean isFalling(Player player) {
        return !player.onGround() && player.getDeltaMovement().y < 0.0;
    }

    // Gleiche Erkennung wie in der windspear-Mod.
    private static boolean isSpear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_spear");
    }
}