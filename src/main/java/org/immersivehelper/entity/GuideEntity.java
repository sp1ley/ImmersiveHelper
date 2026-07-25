package org.immersivehelper.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import org.immersivehelper.data.AliceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public final class GuideEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_WEAPON_DRAWN =
            SynchedEntityData.defineId(GuideEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int BAG_SIZE = 18;
    private static final int PICKUP_SCAN_INTERVAL = 20;
    private static final int COMBAT_TARGET_UPDATE_INTERVAL = 5;
    private static final int FOOD_COOLDOWN_TICKS = 20 * 30;
    private static final int INTERACTION_COOLDOWN_TICKS = 10;
    private static final int COMBAT_MEMORY_TICKS = 20 * 10;
    private static final int WEAPON_STOW_DELAY_TICKS = 20 * 3;
    private static final double MAX_COMBAT_DISTANCE_SQ = 32.0 * 32.0;
    private static final double STAY_DEFENSE_RADIUS_SQ = 10.0 * 10.0;
    private static final double PICKUP_RADIUS = 6.0;
    private static final double FOLLOW_START_DISTANCE_SQ = 25.0;
    private static final double FOLLOW_STOP_DISTANCE_SQ = 9.0;
    private static final double TELEPORT_DISTANCE_SQ = 24.0 * 24.0;

    private static final String TAG_OWNER = "AliceOwner";
    private static final String TAG_STAYING = "AliceStaying";
    private static final String TAG_GREETED = "AliceGreeted";
    private static final String TAG_BAG = "AliceBag";
    private static final String TAG_BAG_FULL_NOTIFIED = "AliceBagFullNotified";
    private static final String TAG_HAS_STAY_POSITION = "AliceHasStayPosition";
    private static final String TAG_STAY_X = "AliceStayX";
    private static final String TAG_STAY_Y = "AliceStayY";
    private static final String TAG_STAY_Z = "AliceStayZ";

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.guide.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.guide.walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.guide.attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer bag = new SimpleContainer(BAG_SIZE);

    private UUID ownerUuid;
    private boolean staying;
    private boolean greeted;
    private boolean bagFullNotified;
    private boolean deathHandled;
    private BlockPos stayPosition;
    private int foodCooldown;
    private int interactionCooldown;
    private int teleportMessageCooldown;
    private int weaponHoldTicks;

    public GuideEntity(EntityType<? extends GuideEntity> entityType, Level level) {
        super(entityType, level);
        setCustomName(Component.translatable("entity.immersive_helper.guide"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05, true));
        goalSelector.addGoal(2, new ReturnToStayGoal());
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(4, new PickUpItemsGoal());
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendSelfGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WEAPON_DRAWN, false);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (staying && stayPosition == null) {
            stayPosition = blockPosition();
        }
        super.customServerAiStep(level);

        if (foodCooldown > 0) {
            foodCooldown--;
        }
        if (interactionCooldown > 0) {
            interactionCooldown--;
        }
        if (teleportMessageCooldown > 0) {
            teleportMessageCooldown--;
        }

        validateCurrentCombatTarget();
        if (tickCount % COMBAT_TARGET_UPDATE_INTERVAL == 0) {
            updateCombatTarget();
        }
        updateWeaponState();
        if (tickCount % 10 == 0) {
            tryGiveEmergencyFood(level);
        }
        if (ownerUuid != null && tickCount % 20 == 0) {
            AliceRegistry.get(level.getServer()).track(ownerUuid, getUUID(), level);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.immersive_helper.alice.not_owner"));
            return InteractionResult.FAIL;
        }
        if (interactionCooldown > 0) {
            return InteractionResult.CONSUME;
        }

        staying = !staying;
        stayPosition = staying ? blockPosition() : null;
        interactionCooldown = INTERACTION_COOLDOWN_TICKS;
        getNavigation().stop();
        setTarget(null);
        player.sendSystemMessage(Component.translatable(staying
                ? "message.immersive_helper.alice.stay"
                : "message.immersive_helper.alice.follow"));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void push(Entity entity) {
        if (!isOwner(entity)) {
            super.push(entity);
        }
    }

    @Override
    protected void doPush(Entity entity) {
        if (!isOwner(entity)) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living)) {
            return false;
        }
        boolean attacked = super.doHurtTarget(level, target);
        if (attacked) {
            markCombatActive();
            triggerAnim("attack_controller", "attack");
        }
        return attacked;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        boolean directSelfDefense = target == getLastHurtByMob()
                && isRecent(getLastHurtByMobTimestamp());
        return isValidCombatTarget(target, directSelfDefense)
                && isWithinAllowedCombatArea(target)
                && super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!deathHandled && level() instanceof ServerLevel level) {
            deathHandled = true;
            setWeaponDrawn(false);
            if (ownerUuid != null) {
                AliceRegistry.get(level.getServer()).remove(ownerUuid, getUUID());
            }
            dropBag(level, getX(), getY() + 0.25, getZ());
            Player owner = getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("message.immersive_helper.alice.death"));
            }
        }
        super.die(damageSource);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerUuid != null) {
            output.putString(TAG_OWNER, ownerUuid.toString());
        }
        output.putBoolean(TAG_STAYING, staying);
        output.putBoolean(TAG_GREETED, greeted);
        output.putBoolean(TAG_BAG_FULL_NOTIFIED, bagFullNotified);
        output.putBoolean(TAG_HAS_STAY_POSITION, stayPosition != null);
        if (stayPosition != null) {
            output.putInt(TAG_STAY_X, stayPosition.getX());
            output.putInt(TAG_STAY_Y, stayPosition.getY());
            output.putInt(TAG_STAY_Z, stayPosition.getZ());
        }
        bag.storeAsItemList(output.list(TAG_BAG, ItemStack.CODEC));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerUuid = parseUuid(input.getStringOr(TAG_OWNER, ""));
        staying = input.getBooleanOr(TAG_STAYING, false);
        greeted = input.getBooleanOr(TAG_GREETED, false);
        bagFullNotified = input.getBooleanOr(TAG_BAG_FULL_NOTIFIED, false);
        stayPosition = input.getBooleanOr(TAG_HAS_STAY_POSITION, false)
                ? new BlockPos(
                        input.getIntOr(TAG_STAY_X, getBlockX()),
                        input.getIntOr(TAG_STAY_Y, getBlockY()),
                        input.getIntOr(TAG_STAY_Z, getBlockZ())
                )
                : null;
        weaponHoldTicks = 0;
        setWeaponDrawn(false);
        bag.clearContent();
        bag.fromItemList(input.listOrEmpty(TAG_BAG, ItemStack.CODEC));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GuideEntity>("locomotion", 5, test ->
                test.setAndContinue(test.isMoving() ? WALK_ANIMATION : IDLE_ANIMATION)
        ));
        controllers.add(new AnimationController<GuideEntity>("attack_controller", 1, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public void setOwner(Player player) {
        ownerUuid = player.getUUID();
    }

    public Player getOwner() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public boolean isOwnedBy(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    public boolean isStaying() {
        return staying;
    }

    public boolean isWeaponDrawn() {
        return entityData.get(DATA_WEAPON_DRAWN);
    }

    public void sendGreeting(ServerPlayer player) {
        if (!greeted) {
            greeted = true;
            player.sendSystemMessage(Component.translatable("message.immersive_helper.alice.greeting"));
        }
    }

    public int getOccupiedBagSlots() {
        int occupied = 0;
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            if (!bag.getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    public int getBagItemCount() {
        int count = 0;
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            count += bag.getItem(slot).getCount();
        }
        return count;
    }

    public TransferResult transferBagTo(ServerPlayer player) {
        int transferred = 0;
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            ItemStack stored = bag.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }

            ItemStack offered = stored.copy();
            int before = offered.getCount();
            player.getInventory().add(offered);
            int moved = before - offered.getCount();
            transferred += moved;
            bag.setItem(slot, offered);
        }
        bagFullNotified = false;
        bag.setChanged();
        return new TransferResult(transferred, 0);
    }

    public TransferResult returnAndDropAll(ServerPlayer player) {
        int transferred = transferBagTo(player).transferred();
        int dropped = 0;
        ServerLevel level = player.level();
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            ItemStack stored = bag.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stored.copy());
            item.setTarget(player.getUUID());
            item.setPickUpDelay(20);
            if (level.addFreshEntity(item)) {
                dropped += stored.getCount();
                bag.setItem(slot, ItemStack.EMPTY);
            }
        }
        bag.setChanged();
        return new TransferResult(transferred, dropped);
    }

    public static Optional<Vec3> findSafePosition(
            ServerLevel level,
            Entity entity,
            BlockPos center,
            int minimumRadius,
            int maximumRadius
    ) {
        for (int radius = minimumRadius; radius <= maximumRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    for (int y = 2; y >= -2; y--) {
                        BlockPos feet = center.offset(x, y, z);
                        if (isSafeAt(level, entity, feet)) {
                            return Optional.of(new Vec3(
                                    feet.getX() + 0.5,
                                    feet.getY(),
                                    feet.getZ() + 0.5
                            ));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafeAt(ServerLevel level, Entity entity, BlockPos feet) {
        BlockPos ground = feet.below();
        if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        Vec3 target = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        Vec3 delta = target.subtract(entity.position());
        AABB targetBox = entity.getBoundingBox().move(delta);
        return level.noCollision(entity, targetBox);
    }

    private void updateCombatTarget() {
        LivingEntity attacker = getLastHurtByMob();
        if (isRecent(getLastHurtByMobTimestamp())
                && isValidCombatTarget(attacker, true)
                && isWithinAllowedCombatArea(attacker)) {
            setTarget(attacker);
            return;
        }
        if (staying) {
            return;
        }

        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        if (owner.tickCount - owner.getLastHurtByMobTimestamp() <= COMBAT_MEMORY_TICKS
                && isValidCombatTarget(ownerAttacker, false)
                && isWithinAllowedCombatArea(ownerAttacker)) {
            setTarget(ownerAttacker);
            return;
        }
        LivingEntity ownerVictim = owner.getLastHurtMob();
        if (owner.tickCount - owner.getLastHurtMobTimestamp() <= COMBAT_MEMORY_TICKS
                && isValidCombatTarget(ownerVictim, false)
                && isWithinAllowedCombatArea(ownerVictim)) {
            setTarget(ownerVictim);
        }
    }

    private void validateCurrentCombatTarget() {
        LivingEntity current = getTarget();
        if (current != null && !canAttack(current)) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    private boolean isWithinAllowedCombatArea(LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (staying) {
            return stayPosition != null
                    && target.position().distanceToSqr(Vec3.atCenterOf(stayPosition)) <= STAY_DEFENSE_RADIUS_SQ;
        }
        return distanceToSqr(target) <= MAX_COMBAT_DISTANCE_SQ;
    }

    private void markCombatActive() {
        weaponHoldTicks = WEAPON_STOW_DELAY_TICKS;
        setWeaponDrawn(true);
    }

    private void updateWeaponState() {
        LivingEntity target = getTarget();
        if (target != null && canAttack(target)) {
            markCombatActive();
            return;
        }
        if (weaponHoldTicks > 0) {
            weaponHoldTicks--;
        }
        if (weaponHoldTicks == 0) {
            setWeaponDrawn(false);
        }
    }

    private void setWeaponDrawn(boolean drawn) {
        if (entityData.get(DATA_WEAPON_DRAWN) != drawn) {
            entityData.set(DATA_WEAPON_DRAWN, drawn);
        }
    }

    private boolean isRecent(int timestamp) {
        return timestamp > 0 && tickCount - timestamp <= COMBAT_MEMORY_TICKS;
    }

    private boolean isValidCombatTarget(LivingEntity target, boolean directSelfDefense) {
        if (target == null
                || !target.isAlive()
                || target == this
                || target instanceof Player
                || isOwner(target)) {
            return false;
        }
        Player owner = getOwner();
        if (owner != null && target instanceof OwnableEntity ownable && owner.equals(ownable.getOwner())) {
            return false;
        }
        return directSelfDefense || target instanceof Enemy;
    }

    private boolean isOwner(Entity entity) {
        return ownerUuid != null && ownerUuid.equals(entity.getUUID());
    }

    private void collectItem(ItemEntity item) {
        if (!canCollect(item)) {
            return;
        }
        ItemStack remainder = bag.addItem(item.getItem().copy());
        if (remainder.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remainder);
        }
        bag.setChanged();
        notifyIfItemDoesNotFit(remainder);
    }

    private boolean canCollect(ItemEntity item) {
        return item.isAlive()
                && !item.hasPickUpDelay()
                && item.getOwner() != this
                && bag.canAddItem(item.getItem());
    }

    private void notifyIfItemDoesNotFit(ItemStack remainder) {
        boolean blocked = !remainder.isEmpty() && !bag.canAddItem(remainder);
        if (blocked && !bagFullNotified) {
            bagFullNotified = true;
            Player owner = getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("message.immersive_helper.alice.bag_full"));
            }
        } else if (!blocked) {
            bagFullNotified = false;
        }
    }

    private void tryGiveEmergencyFood(ServerLevel level) {
        if (foodCooldown > 0) {
            return;
        }
        Player owner = getOwner();
        boolean ownerIsHungry = owner != null && owner.getFoodData().getFoodLevel() <= 2;
        boolean ownerIsBadlyHurt = owner != null && owner.getHealth() <= 6.0F;
        if (owner == null
                || !owner.isAlive()
                || owner.isCreative()
                || owner.isSpectator()
                || (!ownerIsHungry && !ownerIsBadlyHurt)
                || distanceToSqr(owner) > 12.0 * 12.0) {
            return;
        }

        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            ItemStack stored = bag.getItem(slot);
            if (!isOrdinaryFood(stored)) {
                continue;
            }

            ItemStack offered = stored.copyWithCount(1);
            playerInventoryOrDrop(level, owner, offered);
            if (!offered.isEmpty()) {
                return;
            }

            stored.shrink(1);
            if (stored.isEmpty()) {
                bag.setItem(slot, ItemStack.EMPTY);
            }
            bag.setChanged();
            bagFullNotified = false;
            foodCooldown = FOOD_COOLDOWN_TICKS;
            owner.sendSystemMessage(Component.translatable(ownerIsHungry
                    ? "message.immersive_helper.alice.food_hungry"
                    : "message.immersive_helper.alice.food"));
            return;
        }
    }

    private static boolean isOrdinaryFood(ItemStack stack) {
        if (stack.isEmpty() || stack.get(DataComponents.FOOD) == null) {
            return false;
        }
        return stack.getItem() != Items.GOLDEN_APPLE
                && stack.getItem() != Items.ENCHANTED_GOLDEN_APPLE
                && stack.getItem() != Items.CHORUS_FRUIT
                && stack.getItem() != Items.SUSPICIOUS_STEW
                && stack.getItem() != Items.POISONOUS_POTATO
                && stack.getItem() != Items.PUFFERFISH
                && stack.getItem() != Items.ROTTEN_FLESH
                && stack.getItem() != Items.SPIDER_EYE;
    }

    private void playerInventoryOrDrop(ServerLevel level, Player player, ItemStack offered) {
        player.getInventory().add(offered);
        if (offered.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(
                level,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                offered.copy()
        );
        item.setTarget(player.getUUID());
        item.setThrower(this);
        item.setPickUpDelay(100);
        if (level.addFreshEntity(item)) {
            offered.setCount(0);
        }
    }

    private void dropBag(ServerLevel level, double x, double y, double z) {
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            ItemStack stored = bag.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(
                    level,
                    x,
                    y,
                    z,
                    stored.copy(),
                    random.nextGaussian() * 0.05,
                    0.15,
                    random.nextGaussian() * 0.05
            );
            level.addFreshEntity(item);
            bag.setItem(slot, ItemStack.EMPTY);
        }
        bag.setChanged();
    }

    private static UUID parseUuid(String value) {
        try {
            return value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record TransferResult(int transferred, int dropped) {
    }

    private final class DefendSelfGoal extends HurtByTargetGoal {
        private DefendSelfGoal() {
            super(GuideEntity.this);
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            LivingEntity attacker = getLastHurtByMob();
            return isRecent(getLastHurtByMobTimestamp())
                    && isValidCombatTarget(attacker, true)
                    && isWithinAllowedCombatArea(attacker);
        }
    }

    private final class ReturnToStayGoal extends Goal {
        private int pathRecalculation;

        private ReturnToStayGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return staying
                    && stayPosition != null
                    && getTarget() == null
                    && distanceToSqr(Vec3.atCenterOf(stayPosition)) > 2.25;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            pathRecalculation = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (stayPosition != null && --pathRecalculation <= 0) {
                pathRecalculation = 10;
                getNavigation().moveTo(
                        stayPosition.getX() + 0.5,
                        stayPosition.getY(),
                        stayPosition.getZ() + 0.5,
                        1.0
                );
            }
        }
    }

    private final class FollowOwnerGoal extends Goal {
        private Player owner;
        private int pathRecalculation;

        private FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = getOwner();
            return !staying
                    && getTarget() == null
                    && owner != null
                    && owner.isAlive()
                    && !owner.isSpectator()
                    && distanceToSqr(owner) > FOLLOW_START_DISTANCE_SQ;
        }

        @Override
        public boolean canContinueToUse() {
            return !staying
                    && getTarget() == null
                    && owner != null
                    && owner.isAlive()
                    && distanceToSqr(owner) > FOLLOW_STOP_DISTANCE_SQ;
        }

        @Override
        public void start() {
            pathRecalculation = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            owner = null;
        }

        @Override
        public void tick() {
            if (owner == null) {
                return;
            }
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
            if (distanceToSqr(owner) >= TELEPORT_DISTANCE_SQ && level() instanceof ServerLevel level) {
                Optional<Vec3> safe = findSafePosition(level, GuideEntity.this, owner.blockPosition(), 2, 4);
                if (safe.isPresent()) {
                    Vec3 target = safe.get();
                    getNavigation().stop();
                    teleportTo(target.x, target.y, target.z);
                    if (teleportMessageCooldown == 0) {
                        owner.sendSystemMessage(Component.translatable(
                                "message.immersive_helper.alice.teleported"
                        ));
                        teleportMessageCooldown = 20 * 60 * 5;
                    }
                }
                return;
            }
            if (--pathRecalculation <= 0) {
                pathRecalculation = 10;
                getNavigation().moveTo(owner, 1.0);
            }
        }
    }

    private final class PickUpItemsGoal extends Goal {
        private ItemEntity targetItem;
        private int scanCooldown;
        private int pursuitTicks;
        private int pathRecalculation;

        private PickUpItemsGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (staying || getTarget() != null) {
                return false;
            }
            if (scanCooldown-- > 0) {
                return false;
            }
            scanCooldown = PICKUP_SCAN_INTERVAL;

            AABB area = getBoundingBox().inflate(PICKUP_RADIUS, 2.0, PICKUP_RADIUS);
            targetItem = level().getEntitiesOfClass(ItemEntity.class, area, item ->
                            canCollect(item)
                                    && getSensing().hasLineOfSight(item))
                    .stream()
                    .min(Comparator.comparingDouble(GuideEntity.this::distanceToSqr))
                    .orElse(null);
            return targetItem != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !staying
                    && getTarget() == null
                    && targetItem != null
                    && pursuitTicks < 100
                    && canCollect(targetItem);
        }

        @Override
        public void start() {
            pursuitTicks = 0;
            pathRecalculation = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            targetItem = null;
        }

        @Override
        public void tick() {
            pursuitTicks++;
            if (targetItem == null) {
                return;
            }
            getLookControl().setLookAt(targetItem, 10.0F, getMaxHeadXRot());
            if (distanceToSqr(targetItem) <= 2.25) {
                collectItem(targetItem);
                targetItem = null;
                getNavigation().stop();
                return;
            }
            if (--pathRecalculation <= 0) {
                pathRecalculation = 10;
                if (!getNavigation().moveTo(targetItem, 1.0)) {
                    pursuitTicks += 20;
                }
            }
        }
    }
}
