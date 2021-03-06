/*
 * Copyright 2017-2018 asanetargoss
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

package targoss.hardcorealchemy.util;

import javax.annotation.Nonnull;

import ladysnake.dissolution.common.capabilities.CapabilityIncorporealHandler;
import ladysnake.dissolution.common.capabilities.IIncorporealHandler;
import mchorse.metamorph.api.MorphAPI;
import mchorse.metamorph.api.MorphManager;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.Optional;
import targoss.hardcorealchemy.HardcoreAlchemy;
import targoss.hardcorealchemy.ModState;
import targoss.hardcorealchemy.capability.humanity.ICapabilityHumanity;
import targoss.hardcorealchemy.capability.humanity.LostMorphReason;
import targoss.hardcorealchemy.capability.instincts.ICapabilityInstinct;
import targoss.hardcorealchemy.instinct.InstinctAttackPreyOnly;
import targoss.hardcorealchemy.instinct.Instincts;
import targoss.hardcorealchemy.instinct.Instincts.InstinctFactory;
import targoss.hardcorealchemy.item.Items;
import targoss.hardcorealchemy.listener.ListenerPlayerDiet;
import targoss.hardcorealchemy.listener.ListenerPlayerHumanity;

public class MorphState {
    @CapabilityInject(ICapabilityHumanity.class)
    public static final Capability<ICapabilityHumanity> HUMANITY_CAPABILITY = null;
    
    @CapabilityInject(ICapabilityInstinct.class)
    public static final Capability<ICapabilityInstinct> INSTINCT_CAPABILITY = null;

    public static float[] PLAYER_WIDTH_HEIGHT = new float[]{ 0.6F, 1.8F };
    
    public static AbstractMorph createMorph(String morphName) {
        return createMorph(morphName, new NBTTagCompound());
    }
    
    public static AbstractMorph createMorph(String morphName, @Nonnull NBTTagCompound morphProperties) {
        morphProperties.setString("Name", morphName);
        return MorphManager.INSTANCE.morphFromNBT(morphProperties);
    }
    
    public static boolean forceForm(EntityPlayer player, LostMorphReason reason,
            String morphName) {
        return forceForm(player, reason, createMorph(morphName));
    }

    public static boolean forceForm(EntityPlayer player, LostMorphReason reason,
            String morphName, NBTTagCompound morphProperties) {
        return forceForm(player, reason, createMorph(morphName, morphProperties));
    }
    
    /*TODO: Consider making forceForm server-side authoritative, and
     * send special packets to the client, to avoid desyncs at a critical
     * state transition.
     */
    /**
     * Forces the player into the given AbstractMorph (null permitted)
     * with the given reason, and updates the player's needs and instincts
     * Returns true if successful
     */
    public static boolean forceForm(EntityPlayer player, LostMorphReason reason,
            AbstractMorph morph) {
        IMorphing morphing = player.getCapability(ListenerPlayerHumanity.MORPHING_CAPABILITY, null);
        if (morphing == null) {
            return false;
        }
        ICapabilityHumanity capabilityHumanity = player.getCapability(ListenerPlayerHumanity.HUMANITY_CAPABILITY, null);
        if (capabilityHumanity == null) {
            return false;
        }
        
        boolean success = true;
        
        AbstractMorph currentMorph = morphing.getCurrentMorph();
        if ((currentMorph == null && morph != null) ||
            (currentMorph != null && morph == null) ||
                (currentMorph != null && morph != null &&
                 !currentMorph.equals(morph))) {
            success = MorphAPI.morph(player, morph, true);
        }
        
        if (success) {
            capabilityHumanity.loseMorphAbilityFor(reason);
            double humanity = morph == null ? 20.0D : 0.0D;
            capabilityHumanity.setHumanity(humanity);
            if (reason != LostMorphReason.LOST_HUMANITY) {
                // Prevent showing the player a message that their humanity has changed
                capabilityHumanity.setLastHumanity(humanity);
            }
            
            if (ModState.isNutritionLoaded) {
                ListenerPlayerDiet.updateMorphDiet(player);
            }
            
            ICapabilityInstinct instincts = player.getCapability(INSTINCT_CAPABILITY, null);
            if (instincts != null && (morph instanceof EntityMorph)) {
                instincts.setInstinct(ICapabilityInstinct.DEFAULT_INSTINCT_VALUE);
                MorphState.buildInstincts(instincts, ((EntityMorph)morph).getEntity(player.world));
            }
        }
        
        return success;
    }
    
    //TODO: canMorph utility function
    
    public static boolean canUseHighMagic(EntityPlayer player) {
        IMorphing morphing = Morphing.get(player);
        if (morphing == null || morphing.getCurrentMorph() == null) {
            return true;
        }
        
        ICapabilityHumanity humanity = player.getCapability(HUMANITY_CAPABILITY, null);
        if (humanity == null || humanity.canMorph()) {
            return true;
        }
        
        if (player.getActivePotionEffect(Items.POTION_ALLOW_MAGIC) != null) {
            return true;
        }
        
        return false;
    }

    @Optional.Method(modid = ModState.DISSOLUTION_ID)
    public static boolean isIncorporeal(EntityPlayer player) {
        IIncorporealHandler incorporeal = player.getCapability(CapabilityIncorporealHandler.CAPABILITY_INCORPOREAL,
                null);
        if (incorporeal != null && incorporeal.isIncorporeal()) {
            return true;
        }
        return false;
    }

    public static void buildInstincts(ICapabilityInstinct instincts, EntityLivingBase morphEntity) {
        instincts.clearInstincts();
        
        if (morphEntity == null || !(morphEntity instanceof EntityLiving)) {
            return;
        }
        EntityLiving morphedLiving = (EntityLiving)morphEntity;
        
        for (InstinctFactory instinctFactory : Instincts.REGISTRY.getValues()) {
            // No caching right now. Not many instincts to deal with.
            if (instinctFactory.instinctObject.doesMorphEntityHaveInstinct(morphEntity)) {
                instincts.addInstinct(instinctFactory, morphEntity);
            }
        }
    }

}
