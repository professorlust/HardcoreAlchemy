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

package targoss.hardcorealchemy.listener;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import targoss.hardcorealchemy.capability.combatlevel.CapabilityCombatLevel;
import targoss.hardcorealchemy.capability.combatlevel.ICapabilityCombatLevel;
import targoss.hardcorealchemy.capability.combatlevel.ProviderCombatLevel;
import targoss.hardcorealchemy.config.Configs;
import targoss.hardcorealchemy.event.EventLivingAttack;
import targoss.hardcorealchemy.util.MobLevelRange;
import targoss.hardcorealchemy.util.MobLists;

public class ListenerMobLevel extends ConfiguredListener {
    public ListenerMobLevel(Configs configs) {
        super(configs);
    }
    
    @CapabilityInject(ICapabilityCombatLevel.class)
    public static Capability<ICapabilityCombatLevel> COMBAT_LEVEL_CAPABILITY = null;
    public static final ResourceLocation COMBAT_LEVEL_RESOURCE_LOCATION = CapabilityCombatLevel.RESOURCE_LOCATION;
    
    public static Set<String> levelBlacklist = new HashSet();
    
    static {
        levelBlacklist.addAll(MobLists.getBosses());
        levelBlacklist.addAll(MobLists.getNonMobs());
        levelBlacklist.addAll(MobLists.getHumans());
    }
    
    @SubscribeEvent
    public void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)) {
            World world = entity.world;
            if (world != null && world.isRemote) {
                return;
            }
            if (!levelBlacklist.contains(EntityList.getEntityString(entity))) {
                event.addCapability(COMBAT_LEVEL_RESOURCE_LOCATION, new ProviderCombatLevel());
                }
        }
        
    }
    
    @SubscribeEvent
    public void onCheckMobHasLevel(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase entityLiving = (EntityLivingBase)entity;
        
        ICapabilityCombatLevel combatLevel = entityLiving.getCapability(COMBAT_LEVEL_CAPABILITY, null);
        if (combatLevel != null && !combatLevel.getHasCombatLevel()) {
            combatLevel.setHasCombatLevel(true);
            MobLevelRange levelRange = MobLevelRange.getRange(entityLiving.dimension, entityLiving.posY);
            //TODO: better random level algorithm
            int level = levelRange.getRandomLevel(entityLiving.posX, entityLiving.posZ, entityLiving.world.getSeed());
            combatLevel.setValue(level);
        }
    }
    
    
    @SubscribeEvent
    public void onLivingHurt(EventLivingAttack event) {
        DamageSource source = event.source;
        Entity entity = source.getEntity();
        if (entity == null) {
            // ¯\_(ツ)_/¯
            return;
        }
        World world = entity.world;
        if (world != null && world.isRemote) {
            return;
        }
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            return;
        }
        
        EntityLivingBase attacker = (EntityLivingBase)source.getEntity();
        EntityLivingBase defender = event.entity;
        
        boolean attackerIsPlayer = attacker instanceof EntityPlayer;
        boolean defenderIsPlayer = defender instanceof EntityPlayer;
        if (attackerIsPlayer && defenderIsPlayer) {
            return;
        }
        
        int attackerLevel = 0;
        int defenderLevel = 0;
        
        if (attackerIsPlayer) {
            attackerLevel = ((EntityPlayer)attacker).experienceLevel;
        }
        else if (attacker.hasCapability(COMBAT_LEVEL_CAPABILITY, null)) {
            attackerLevel = attacker.getCapability(COMBAT_LEVEL_CAPABILITY, null).getValue();
        }
        else {
            return;
        }
        if (defenderIsPlayer) {
            defenderLevel = ((EntityPlayer)defender).experienceLevel;
        }
        else if (defender.hasCapability(COMBAT_LEVEL_CAPABILITY, null)) {
            defenderLevel = defender.getCapability(COMBAT_LEVEL_CAPABILITY, null).getValue();
        }
        else {
            return;
        }
        
        float hurtMultiplier = CapabilityCombatLevel.getDamageMultiplier(attackerLevel, defenderLevel);
        event.amount = event.amount * hurtMultiplier;
    }
}
