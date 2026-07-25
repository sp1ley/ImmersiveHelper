package dev.sp1ley.immersivehelper.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sp1ley.immersivehelper.ImmersiveHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AliceRegistry extends SavedData {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, AliceRecord>> ENTRIES_CODEC =
            Codec.unboundedMap(UUID_CODEC, AliceRecord.CODEC);
    private static final Codec<AliceRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.fieldOf("entries").forGetter(registry -> registry.entries)
    ).apply(instance, AliceRegistry::new));
    private static final SavedDataType<AliceRegistry> TYPE = new SavedDataType<>(
            ImmersiveHelper.id("alice_registry"),
            AliceRegistry::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, AliceRecord> entries;

    public AliceRegistry() {
        this(new HashMap<>());
    }

    private AliceRegistry(Map<UUID, AliceRecord> entries) {
        this.entries = new HashMap<>(entries);
    }

    public static AliceRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<AliceRecord> find(UUID ownerUuid) {
        return Optional.ofNullable(entries.get(ownerUuid));
    }

    public void track(UUID ownerUuid, UUID entityUuid, ServerLevel level) {
        AliceRecord updated = new AliceRecord(entityUuid, level.dimension().identifier());
        if (!updated.equals(entries.put(ownerUuid, updated))) {
            setDirty();
        }
    }

    public void remove(UUID ownerUuid, UUID entityUuid) {
        AliceRecord current = entries.get(ownerUuid);
        if (current != null && current.entityUuid().equals(entityUuid)) {
            entries.remove(ownerUuid);
            setDirty();
        }
    }

    public record AliceRecord(UUID entityUuid, Identifier dimension) {
        private static final Codec<AliceRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("entity_uuid").forGetter(AliceRecord::entityUuid),
                Identifier.CODEC.fieldOf("dimension").forGetter(AliceRecord::dimension)
        ).apply(instance, AliceRecord::new));
    }
}
