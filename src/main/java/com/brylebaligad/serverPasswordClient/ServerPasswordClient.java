package com.brylebaligad.serverPasswordClient;

import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ServerPasswordClient extends JavaPlugin implements Listener, PluginMessageListener {
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("ServerPassword [Proxy Edition Client] (c) Bryle Baligad, 2023 - 2026");
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "serverpassword:auth");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "serverpassword:auth", this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        authenticatedPlayers.clear();
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player _player, byte @NotNull [] message) {
        if (channel.equals(("serverpassword:auth"))) {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
                String subChannel = in.readUTF();
                if (subChannel.equals("update")) {
                    UUID uuid = UUID.fromString(in.readUTF());
                    boolean isAuthenticated = in.readBoolean();

                    if (getServer().getPlayer(uuid) != null) {
                        getLogger().info("Auth update for " + Objects.requireNonNull(getServer().getPlayer(uuid)).getName() + ": " + isAuthenticated);
                    }
                    if (isAuthenticated) {
                        authenticatedPlayers.add(uuid);
                        Player p = getServer().getPlayer(uuid);
                        if (p != null) {
                            Title t = Title.title(Component.text(""), Component.text(""));
                            getServer().getScheduler().runTaskLater(this, () -> p.showTitle(t), 5L);
                        }
                    } else {
                        authenticatedPlayers.remove(uuid);
                    }
                }

                if (subChannel.equals("title")) {
                    UUID uuid = UUID.fromString(in.readUTF());
                    String title = in.readUTF();

                    Player player = getServer().getPlayer(uuid);
                    if (player != null) {
                        getLogger().info("Send title for " + Objects.requireNonNull(getServer().getPlayer(uuid)).getName() + ": " + title);
                        String mainTitle = title.split("\n")[0];
                        String subTitle = title.split("\n")[1];
                        Title t = Title.title(Component.text(mainTitle), Component.text(subTitle));
                        getServer().getScheduler().runTaskLater(this, () -> player.showTitle(t), 5L);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        authenticatedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().setSneaking(false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        try {
            if (Objects.requireNonNull(event.getTarget()).getType() != EntityType.PLAYER) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }

        Player player = (Player) event.getTarget();

        if (!authenticatedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        try {
            if (Objects.requireNonNull(event.getTarget()).getType() != EntityType.PLAYER) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }

        Player player = (Player) event.getTarget();

        if (!authenticatedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickBlock(PlayerPickItemEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickUp(EntityPickupItemEvent event) {
        if (event instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!authenticatedPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!authenticatedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

        if (!authenticatedPlayers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

        if (!authenticatedPlayers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract (PlayerInteractEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity (PlayerInteractEntityEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractAtEntity (PlayerInteractAtEntityEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
