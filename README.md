# BorderTeleport Plugin Documentation

**BorderTeleport** is a **Minecraft Spigot plugin** designed for **multi-server BungeeCord setups**. It allows multiple servers to function as **one continuous world** by automatically teleporting players when they cross predefined region boundaries.

With **BorderTeleport**, you can divide a large world across **4 interconnected servers**, creating a seamless experience where players can explore without interruption. When a player reaches the edge of one server, they are instantly transferred to the corresponding position on the neighboring server—maintaining their movement direction and gameplay continuity.

## 🌍 Why Use BorderTeleport?
✅ **Massive Open Worlds** – Split a large Minecraft world into four regions, each running on a separate server.  
✅ **Survival & Adventure Servers** – Connect biomes, continents, or cities across different servers.  
✅ **Seamless Player Experience** – No loading screens or commands; just walk across the border and teleport automatically.  

## 🛑 Current Limitation
- **BorderTeleport currently supports dividing a world into exactly 4 servers**:
  - **Northwest**
  - **Northeast**
  - **Southwest**
  - **Southeast**
- Expansion for more than 4 servers may be considered in future updates.

## 🔗 How It Works
- **Uses BungeeCord plugin messaging** to communicate between servers.
- Prevents players from getting stuck at offline destinations.
- Ensures teleportation keeps movement direction for a natural experience.

🚀 **Designed for high-performance server networks!**
