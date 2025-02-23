package mekanism.common.network.to_server;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import mekanism.api.MekanismAPI;
import mekanism.api.robit.RobitSkin;
import mekanism.api.security.ISecurityUtils;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.entity.EntityRobit;
import mekanism.common.entity.RobitPrideSkinData;
import mekanism.common.network.BasePacketHandler;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.registries.MekanismRobitSkins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PacketRobit implements IMekanismPacket {

    private static final Map<String, List<ResourceKey<RobitSkin>>> EASTER_EGGS = Map.of(
          "sara", getPrideSkins(RobitPrideSkinData.TRANS, RobitPrideSkinData.LESBIAN)
    );

    private static List<ResourceKey<RobitSkin>> getPrideSkins(RobitPrideSkinData... prideSkinData) {
        return Stream.of(prideSkinData).map(MekanismRobitSkins.PRIDE_SKINS::get).toList();
    }

    private final RobitPacketType activeType;
    private final int entityId;
    private final String name;
    private final ResourceKey<RobitSkin> skin;

    public PacketRobit(RobitPacketType type, EntityRobit robit) {
        this(type, robit.getId(), null, null);
    }

    public PacketRobit(EntityRobit robit, @NotNull String name) {
        this(RobitPacketType.NAME, robit, name, null);
    }

    public PacketRobit(EntityRobit robit, @NotNull ResourceKey<RobitSkin> skin) {
        this(RobitPacketType.SKIN, robit, null, skin);
    }

    private PacketRobit(RobitPacketType type, EntityRobit robit, @Nullable String name, @Nullable ResourceKey<RobitSkin> skin) {
        this(type, robit.getId(), name, skin);
    }

    private PacketRobit(RobitPacketType type, int entityId, @Nullable String name, @Nullable ResourceKey<RobitSkin> skin) {
        this.activeType = type;
        this.entityId = entityId;
        this.name = name;
        this.skin = skin;
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        Player player = context.getSender();
        if (player != null) {
            EntityRobit robit = (EntityRobit) player.level().getEntity(entityId);
            if (robit != null && ISecurityUtils.INSTANCE.canAccess(player, robit)) {
                if (activeType == RobitPacketType.GO_HOME) {
                    robit.goHome();
                } else if (activeType == RobitPacketType.FOLLOW) {
                    robit.setFollowing(!robit.getFollowing());
                } else if (activeType == RobitPacketType.DROP_PICKUP) {
                    robit.setDropPickup(!robit.getDropPickup());
                } else if (activeType == RobitPacketType.NAME) {
                    robit.setCustomName(TextComponentUtil.getString(name));
                    if (robit.getSkin() == MekanismRobitSkins.BASE) {
                        //If the robit has the base skin currently equipped
                        List<ResourceKey<RobitSkin>> skins = EASTER_EGGS.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
                        // check if there are any skins paired with the name that got set as an Easter egg
                        if (!skins.isEmpty()) {
                            // if there are, then pick a random one and set it
                            // Note: We use null for the player instead of the actual player in case we ever
                            // end up adding any Easter egg skins that aren't unlocked by default, to still
                            // be able to equip them. We already validate the player can access the robit
                            // above before setting the name
                            robit.setSkin(skins.get(robit.level().random.nextInt(skins.size())), null);
                        }
                    }
                } else if (activeType == RobitPacketType.SKIN) {
                    robit.setSkin(skin, player);
                }
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(activeType);
        buffer.writeVarInt(entityId);
        if (activeType == RobitPacketType.NAME) {
            buffer.writeUtf(name);
        } else if (activeType == RobitPacketType.SKIN) {
            buffer.writeResourceKey(skin);
        }
    }

    public static PacketRobit decode(FriendlyByteBuf buffer) {
        RobitPacketType activeType = buffer.readEnum(RobitPacketType.class);
        int entityId = buffer.readVarInt();
        String name = null;
        ResourceKey<RobitSkin> skin = null;
        if (activeType == RobitPacketType.NAME) {
            name = BasePacketHandler.readString(buffer).trim();
        } else if (activeType == RobitPacketType.SKIN) {
            skin = buffer.readResourceKey(MekanismAPI.ROBIT_SKIN_REGISTRY_NAME);
        }
        return new PacketRobit(activeType, entityId, name, skin);
    }

    public enum RobitPacketType {
        GO_HOME,
        FOLLOW,
        DROP_PICKUP,
        NAME,
        SKIN
    }
}