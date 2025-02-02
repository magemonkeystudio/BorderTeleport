package com.yourplugin.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.AnimalTamer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MountData {
    private final EntityType entityType;
    private final String customName;
    private final double health;
    private final Map<String, Object> attributes;
    private final boolean tamed;
    private final UUID ownerUUID;

    public MountData(Entity mount) {
        this.entityType = mount.getType();
        this.customName = mount.getCustomName();
        this.attributes = new HashMap<>();

        if (mount instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) mount;
            this.health = living.getHealth();

            for (Attribute attribute : Attribute.values()) {
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null) {
                    attributes.put(attribute.name(), instance.getBaseValue());
                }
            }
        } else {
            this.health = 0;
        }

        if (mount instanceof Tameable) {
            Tameable tameable = (Tameable) mount;
            this.tamed = tameable.isTamed();
            this.ownerUUID = tameable.getOwner() != null ? tameable.getOwner().getUniqueId() : null;
        } else {
            this.tamed = false;
            this.ownerUUID = null;
        }
    }

    public Entity recreateMount(World world, Location location) {
        Entity mount = world.spawnEntity(location, entityType);

        if (customName != null) {
            mount.setCustomName(customName);
        }

        if (mount instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) mount;
            living.setHealth(health);

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                try {
                    Attribute attribute = Attribute.valueOf(entry.getKey());
                    AttributeInstance instance = living.getAttribute(attribute);
                    if (instance != null) {
                        instance.setBaseValue((double) entry.getValue());
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid attributes
                }
            }
        }

        if (mount instanceof Tameable && tamed && ownerUUID != null) {
            Tameable tameable = (Tameable) mount;
            tameable.setTamed(true);
            AnimalTamer owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                tameable.setOwner(owner);
            }
        }

        return mount;
    }
}