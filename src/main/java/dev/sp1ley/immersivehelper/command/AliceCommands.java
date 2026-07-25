package dev.sp1ley.immersivehelper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sp1ley.immersivehelper.data.AliceRegistry;
import dev.sp1ley.immersivehelper.entity.GuideEntity;
import dev.sp1ley.immersivehelper.entity.ModEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class AliceCommands {
    private AliceCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher)
        );
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("alice")
                .then(Commands.literal("summon").executes(context -> summon(context.getSource())))
                .then(Commands.literal("dismiss").executes(context -> dismiss(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("collect").executes(context -> collect(context.getSource())))
        );
    }

    private static int summon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (AliceRegistry.get(source.getServer()).find(player.getUUID()).isPresent()
                || findAlice(source.getServer(), player.getUUID()).isPresent()) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.already_exists"));
            return 0;
        }

        ServerLevel level = player.level();
        GuideEntity alice = ModEntities.GUIDE.create(level, EntitySpawnReason.COMMAND);
        if (alice == null) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.summon_failed"));
            return 0;
        }

        Optional<Vec3> position = GuideEntity.findSafePosition(level, alice, player.blockPosition(), 2, 4);
        if (position.isEmpty()) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.no_safe_position"));
            return 0;
        }

        Vec3 spawn = position.get();
        alice.snapTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        alice.setOwner(player);
        alice.setPersistenceRequired();
        if (!level.addFreshEntity(alice)) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.summon_failed"));
            return 0;
        }

        AliceRegistry.get(source.getServer()).track(player.getUUID(), alice.getUUID(), level);
        alice.sendGreeting(player);
        return 1;
    }

    private static int dismiss(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<GuideEntity> alice = findAlice(source.getServer(), player.getUUID());
        if (alice.isEmpty()) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.not_found"));
            return 0;
        }

        GuideEntity guide = alice.get();
        GuideEntity.TransferResult result = guide.returnAndDropAll(player);
        AliceRegistry.get(source.getServer()).remove(player.getUUID(), guide.getUUID());
        guide.discard();
        source.sendSuccess(() -> Component.translatable(
                "command.immersive_helper.alice.dismissed",
                result.transferred(),
                result.dropped()
        ), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<GuideEntity> alice = findAlice(source.getServer(), player.getUUID());
        if (alice.isEmpty()) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.not_found"));
            return 0;
        }

        GuideEntity guide = alice.get();
        Component mode = Component.translatable(guide.isStaying()
                ? "mode.immersive_helper.stay"
                : "mode.immersive_helper.follow");
        source.sendSuccess(() -> Component.translatable(
                "command.immersive_helper.alice.status",
                mode,
                Math.round(guide.getHealth()),
                Math.round(guide.getMaxHealth()),
                guide.getOccupiedBagSlots(),
                guide.getBagItemCount()
        ), false);
        return 1;
    }

    private static int collect(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<GuideEntity> alice = findAlice(source.getServer(), player.getUUID());
        if (alice.isEmpty()) {
            source.sendFailure(Component.translatable("command.immersive_helper.alice.not_found"));
            return 0;
        }

        GuideEntity guide = alice.get();
        GuideEntity.TransferResult result = guide.transferBagTo(player);
        source.sendSuccess(() -> Component.translatable(
                "command.immersive_helper.alice.collected",
                result.transferred(),
                guide.getBagItemCount()
        ), false);
        return result.transferred() > 0 ? 1 : 0;
    }

    private static Optional<GuideEntity> findAlice(MinecraftServer server, UUID ownerUuid) {
        AliceRegistry registry = AliceRegistry.get(server);
        Optional<AliceRegistry.AliceRecord> record = registry.find(ownerUuid);
        if (record.isPresent()) {
            Entity entity = server.overworld().getEntityInAnyDimension(record.get().entityUuid());
            if (entity instanceof GuideEntity guide
                    && guide.isAlive()
                    && guide.isOwnedBy(ownerUuid)) {
                return Optional.of(guide);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof GuideEntity guide
                        && guide.isAlive()
                        && guide.isOwnedBy(ownerUuid)) {
                    registry.track(ownerUuid, guide.getUUID(), level);
                    return Optional.of(guide);
                }
            }
        }
        return Optional.empty();
    }
}
