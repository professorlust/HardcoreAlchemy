/*
 * Copyright 2018 asanetargoss
 * 
 * This file is part of Hardcore Alchemy.
 * 
 * Hardcore Alchemy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation version 3 of the License.
 * 
 * Hardcore Alchemy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Hardcore Alchemy.  If not, see <http://www.gnu.org/licenses/>.
 */

package targoss.hardcorealchemy.listener;

import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import targoss.hardcorealchemy.config.Configs;
import targoss.hardcorealchemy.coremod.CoremodHook;

@SideOnly(Side.CLIENT)
public class ListenerRenderView extends ConfiguredListener {
    public ListenerRenderView(Configs configs) {
        super(configs);
    }
    
    @CapabilityInject(IMorphing.class)
    public static final Capability<IMorphing> MORPHING_CAPABILITY = null;
    
    public static final float DEFAULT_PLAYER_HEIGHT = 1.8F;
    public static final float MIN_VIEW_SCALE_MULTIPLIER = 0.05F;
    public static float zoomScaleMultiplier = 1.0F;
    
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onCheckMorphScale(PlayerTickEvent event) {
        if (event.phase != Phase.END) {
            // We want this to be called just after Metamorph overrides the player scale
            return;
        }
        
        EntityPlayer player = event.player;
        
        float heightForCamera = player.height;
        IMorphing morphing = player.getCapability(MORPHING_CAPABILITY, null);
        if (morphing == null) {
            heightForCamera = DEFAULT_PLAYER_HEIGHT;
        }
        else {
            AbstractMorph morph = morphing.getCurrentMorph();
            if (!(morph instanceof EntityMorph) || ((EntityMorph)morph).getEntity() == null) {
                heightForCamera = DEFAULT_PLAYER_HEIGHT;
            }
        }
        if (heightForCamera <= 0.0F) {
            heightForCamera = 0.0001F;
        }
        
        // Because of trigonometry, doubling the zoom scale will effectively half the camera region, simulating a change in player head size
        float desiredZoomScaleMultiplier = DEFAULT_PLAYER_HEIGHT / heightForCamera;
        if (desiredZoomScaleMultiplier < MIN_VIEW_SCALE_MULTIPLIER) desiredZoomScaleMultiplier = MIN_VIEW_SCALE_MULTIPLIER;
        zoomScaleMultiplier = desiredZoomScaleMultiplier;
    }
    
    /**
     * Change view region according to the size of the current morph. Larger morphs have a bigger view at short distances.
     */
    @SubscribeEvent
    public void onCheckCamera(EntityViewRenderEvent.FOVModifier event) {
        GlStateManager.scale(zoomScaleMultiplier, zoomScaleMultiplier, 1.0D);
    }
    
    /**
     * Change hand render scaling so it's in the correct position of the viewport regardless of morph size
     */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onRenderHand(RenderHandEvent event) {
        float handScaleMultiplier = 1.0f / zoomScaleMultiplier;
        GlStateManager.scale(handScaleMultiplier, handScaleMultiplier, 1.0D);
    }
}
