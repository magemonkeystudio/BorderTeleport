package magemonkey.data;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Registry;
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
    private final Component customName;
    private final double health;
    private final Map<Attribute, Double> attributes;
    private final boolean tamed;
    private final UUID ownerUUID;

    public MountData(Entity mount) {
        if (mount == null) {
            throw new IllegalArgumentException("Mount cannot be null");
        }

        this.entityType = mount.getType();
        this.customName = mount.customName();
        this.attributes = new HashMap<>();

        if (mount instanceof LivingEntity living) {
            this.health = living.getHealth();

            // Store all available attributes for this entity using Registry
            Registry.ATTRIBUTE.forEach(attribute -> {
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null) {
                    attributes.put(attribute, instance.getBaseValue());
                }
            });
        } else {
            this.health = 0;
        }

        if (mount instanceof Tameable tameable) {
            this.tamed = tameable.isTamed();
            this.ownerUUID = tameable.getOwner() != null ? tameable.getOwner().getUniqueId() : null;
        } else {
            this.tamed = false;
            this.ownerUUID = null;
        }
    }

    public Entity recreateMount(World world, Location location) {
        if (world == null || location == null) {
            throw new IllegalArgumentException("World and location must not be null");
        }

        Entity mount = world.spawnEntity(location, entityType);

        if (customName != null) {
            mount.customName(customName);
        }

        if (mount instanceof LivingEntity living) {
            living.setHealth(health);

            // Restore attributes
            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                AttributeInstance instance = living.getAttribute(entry.getKey());
                if (instance != null) {
                    instance.setBaseValue(entry.getValue());
                }
            }
        }

        if (mount instanceof Tameable tameable && tamed && ownerUUID != null) {
            tameable.setTamed(true);
            AnimalTamer owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                tameable.setOwner(owner);
            }
        }

        return mount;
    }

    // Getters
    public EntityType getEntityType() {
        return entityType;
    }

    public Component getCustomName() {
        return customName;
    }

    public double getHealth() {
        return health;
    }

    public Map<Attribute, Double> getAttributes() {
        return new HashMap<>(attributes);
    }

    public boolean isTamed() {
        return tamed;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }
}