package com.newbiewood.nbw.weather;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import com.newbiewood.nbw.ScorchAndFrost;

import java.util.*;

public record WeatherSyncPacket(Map<ClimateZoneGroup, SyncEvent> events) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<WeatherSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ScorchAndFrost.MODID, "weather_sync"));

    public static final StreamCodec<ByteBuf, WeatherSyncPacket> STREAM_CODEC =
            StreamCodec.<ByteBuf, WeatherSyncPacket>of(
                    (buf, packet) -> {
                        buf.writeInt(packet.events.size());
                        for (var entry : packet.events.entrySet())
                        {
                            buf.writeByte(entry.getKey().ordinal());
                            buf.writeFloat(entry.getValue().peakOffset);
                            buf.writeInt(entry.getValue().totalDays);
                            buf.writeInt(entry.getValue().remainingDays);
                            buf.writeByte(entry.getValue().type.ordinal());
                            buf.writeByte(entry.getValue().phase.ordinal());
                        }
                    },
                    (buf) -> {
                        int size = buf.readInt();
                        Map<ClimateZoneGroup, SyncEvent> map = new HashMap<>();
                        for (int i = 0; i < size; i++)
                        {
                            ClimateZoneGroup group = ClimateZoneGroup.values()[buf.readByte() & 0xFF];
                            float peakOffset = buf.readFloat();
                            int totalDays = buf.readInt();
                            int remainingDays = buf.readInt();
                            WeatherEventType type = WeatherEventType.values()[buf.readByte() & 0xFF];
                            EventPhase phase = EventPhase.values()[buf.readByte() & 0xFF];
                            map.put(group, new SyncEvent(type, peakOffset, totalDays, remainingDays, phase));
                        }
                        return new WeatherSyncPacket(map);
                    }
            );

    public static void send(ServerPlayer player, Map<ClimateZoneGroup, WeatherEvent> events)
    {
        Map<ClimateZoneGroup, SyncEvent> syncEvents = new HashMap<>();
        for (var entry : events.entrySet())
        {
            WeatherEvent e = entry.getValue();
            syncEvents.put(entry.getKey(), new SyncEvent(
                    e.getType(), e.getPeakOffset(), e.getTotalDays(),
                    e.getRemainingDays(), e.getCurrentPhase()));
        }
        PacketDistributor.sendToPlayer(player, new WeatherSyncPacket(syncEvents));
    }

    public static void handleClient(WeatherSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        context.enqueueWork(() -> WeatherClientCache.update(packet.events()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public record SyncEvent(WeatherEventType type, float peakOffset, int totalDays, int remainingDays, EventPhase phase) {}
}