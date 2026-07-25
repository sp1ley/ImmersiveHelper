package org.immersivehelper.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.immersivehelper.entity.GuideEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@Environment(EnvType.CLIENT)
final class AliceSwordLayer extends ItemInHandGeoLayer<GuideEntity, Void, LivingEntityRenderState> {
    private static final String WEAPON_BONE = "right_hand_item";

    // Fine-tuning for the custom Alice hand bone is intentionally kept in one place.
    private static final float TRANSLATE_X = 0.0F;
    private static final float TRANSLATE_Y = -0.02F;
    private static final float TRANSLATE_Z = 0.02F;
    private static final float ROTATE_Y_DEGREES = -8.0F;
    private static final float ROTATE_Z_DEGREES = 4.0F;
    private static final float SCALE = 0.82F;

    private ItemStack visualStoneSword;

    AliceSwordLayer(
            EntityRendererProvider.Context context,
            GeoRenderer<GuideEntity, Void, LivingEntityRenderState> renderer
    ) {
        super(context, renderer, WEAPON_BONE, null);
    }

    @Override
    protected List<BlockAndItemGeoLayer.RenderData> getRelevantBones(
            GuideEntity animatable,
            Void relatedObject,
            LivingEntityRenderState renderState,
            float partialTick
    ) {
        if (!animatable.isWeaponDrawn()) {
            return List.of();
        }

        ItemDisplayContext displayContext = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        ItemStackRenderState itemState = RenderUtil.createRenderStateForItem(
                getVisualStoneSword(),
                itemModelResolver,
                displayContext,
                animatable
        );
        return List.of(BlockAndItemGeoLayer.RenderData.item(WEAPON_BONE, displayContext, itemState));
    }

    private ItemStack getVisualStoneSword() {
        if (visualStoneSword == null) {
            visualStoneSword = new ItemStack(Items.STONE_SWORD);
        }

        return visualStoneSword;
    }

    @Override
    protected void submitItemStackRender(
            PoseStack poseStack,
            GeoBone bone,
            ItemStackRenderState itemState,
            ItemDisplayContext displayContext,
            LivingEntityRenderState renderState,
            SubmitNodeCollector submitNodeCollector,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(TRANSLATE_X, TRANSLATE_Y, TRANSLATE_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(ROTATE_Y_DEGREES));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATE_Z_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE);
        super.submitItemStackRender(
                poseStack,
                bone,
                itemState,
                displayContext,
                renderState,
                submitNodeCollector,
                packedLight
        );
        poseStack.popPose();
    }
}
