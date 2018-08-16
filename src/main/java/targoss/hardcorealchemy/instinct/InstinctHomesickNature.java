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

package targoss.hardcorealchemy.instinct;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import targoss.hardcorealchemy.event.EventPlayerMoveWithHeading;
import targoss.hardcorealchemy.network.MessageInstinctHomesickNature;
import targoss.hardcorealchemy.network.PacketHandler;
import targoss.hardcorealchemy.util.Chat;
import targoss.hardcorealchemy.util.MiscVanilla;
import targoss.hardcorealchemy.util.RandomWithPublicSeed;

public class InstinctHomesickNature implements IInstinct {
    public InstinctHomesickNature() {}
    
    public RandomWithPublicSeed random = new RandomWithPublicSeed();
    public long lastSeed = random.getSeed();
    
    // Time until the instinct ends as long as the player is in the desired location (ticks)
    public int timer = INSTINCT_TIME;
    public int directionTimer = MAX_DIRECTION_TIME;
    public float directionAngle = 0.0F;
    
    private static final String NBT_TIMER = "timer";
    private static final String NBT_DIRECTION_TIMER = "directionTimer";
    private static final String NBT_DIRECTION_ANGLE = "directionAngle";

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        
        nbt.setInteger(NBT_TIMER, timer);
        nbt.setInteger(NBT_DIRECTION_TIMER, directionTimer);
        nbt.setFloat(NBT_DIRECTION_ANGLE, directionAngle);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        timer = nbt.getInteger(NBT_TIMER);
        directionTimer = nbt.getInteger(NBT_DIRECTION_TIMER);
        directionAngle = nbt.getFloat(NBT_DIRECTION_ANGLE);
    }

    private static final int INSTINCT_TIME = 300 * 20;
    private static final int MIN_DIRECTION_TIME = 5 * 20;
    private static final int MAX_DIRECTION_TIME = 15 * 20;
    
    public void setRandomDirection(EntityPlayer player) {
        // The client is given the server's seed, to allow player movement to be better predicted ahead of time
        
        directionTimer += MIN_DIRECTION_TIME + random.nextInt(MAX_DIRECTION_TIME - MIN_DIRECTION_TIME);
        // Choose an axis along which the player must move, and change player rotation to align with it
        float directionAngle = random.nextFloat() * 180.0F;
        
        this.directionAngle = directionAngle;
        lastSeed = random.getSeed();
        
        if (!player.world.isRemote) {
            PacketHandler.INSTANCE.sendTo(new MessageInstinctHomesickNature(this), (EntityPlayerMP)player);
        }
    }

    @Override
    public boolean doesMorphEntityHaveInstinct(EntityLivingBase morphEntity) {
        return morphEntity instanceof EntityAnimal;
    }
    
    private boolean areLeavesAbove(World world, BlockPos startPos) {
        int worldHeight = MiscVanilla.getWorldHeight(world);
        if (startPos.getY() >= worldHeight) {
            return false;
        }
        RayTraceResult result = world.rayTraceBlocks(
                new Vec3d(startPos.getX(), startPos.getY(), startPos.getZ()),
                new Vec3d(startPos.getX(), worldHeight, startPos.getZ()),
                true, true, true);
        if (result == null) {
            return false;
        }
        Block block = world.getBlockState(result.getBlockPos()).getBlock();
        return (block != null && !(block instanceof BlockLeaves));
    }
    
    private boolean wereThereLeaves = false;
    private BlockPos nearbyLeafCheck = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private int lastLeafCheck = 0;
    
    private boolean wereLeavesAboveNearby(EntityPlayer player) {
        BlockPos leafCheckStart = player.getPosition().up((int)player.height);
        if (player.ticksExisted < lastLeafCheck + 10 ||
                (Math.abs(leafCheckStart.getX() - nearbyLeafCheck.getX()) < 4 &&
                        Math.abs(leafCheckStart.getY() - nearbyLeafCheck.getY()) < 4 &&
                        Math.abs(leafCheckStart.getZ() - nearbyLeafCheck.getZ()) < 4)) {
            return wereThereLeaves;
        }
        wereThereLeaves = areLeavesAbove(player.world, leafCheckStart);
        lastLeafCheck = player.ticksExisted;
        nearbyLeafCheck = leafCheckStart;
        return wereThereLeaves;
    }
    
    public boolean doesFeelAtHome(EntityPlayer player) {
        World world = player.world;
        
        IBlockState stateBelow = world.getBlockState(player.getPosition().down(1));
        Block blockBelow = stateBelow.getBlock();
        if (blockBelow == null) {
            return false;
        }
        
        return (blockBelow instanceof BlockGrass && (
                        world.canSeeSky(player.getPosition().up((int)player.height)) ||
                        wereLeavesAboveNearby(player)
                ));
    }

    @Override
    public IInstinct createInstanceFromMorphEntity(EntityLivingBase morphEntity) {
        return new InstinctHomesickNature();
    }

    @Override
    public ITextComponent getNeedMessage(EntityPlayer player) {
        return new TextComponentTranslation("hardcorealchemy.instinct.homesick.nature.need");
    }

    @Override
    public ITextComponent getNeedMessageOnActivate(EntityPlayer player) {
        return new TextComponentTranslation("hardcorealchemy.instinct.homesick.activate");
    }

    @Override
    public boolean shouldStayActive(EntityPlayer player) {
        return timer >= 0;
    }

    @Override
    public void onActivate(EntityPlayer player) {
        setRandomDirection(player);
    }

    @Override
    public void onDeactivate(EntityPlayer player) {
        timer = INSTINCT_TIME;
        directionTimer = MAX_DIRECTION_TIME;
    }
    
    // Determines whether the timer was last going up or down (false = down)
    public boolean feltAtHome = false;
    // Determines whether a message has been displayed yet since feltAtHome changed (same as feltAtHome => do not display message)
    boolean acknowledgedFeelingAtHome = false;
    private static final int TIMER_HYSTERESIS_DOWN = 2 * 20;
    private static final int TIMER_HYSTERESIS_UP = 10 * 20;
    // Determines the time threshold for displaying a message about need fulfillment
    private int timerHysteresisMark = INSTINCT_TIME;

    @Override
    public void tick(EntityPlayer player) {
        boolean feelsAtHome = doesFeelAtHome(player);
        if (feelsAtHome) {
            --timer;
        }
        else if (timer < INSTINCT_TIME) {
            ++timer;
        }
        if (feelsAtHome) {
            if (acknowledgedFeelingAtHome && timer > timerHysteresisMark + TIMER_HYSTERESIS_UP) {
                if (player.world.isRemote) {
                    Chat.messageSP(Chat.Type.NOTIFY, player, new TextComponentTranslation("hardcorealchemy.instinct.homesick.nature.not_at_home"));
                }
                acknowledgedFeelingAtHome = false;
            }
        }
        else {
            if (!acknowledgedFeelingAtHome && timer < timerHysteresisMark - TIMER_HYSTERESIS_DOWN) {
                if (player.world.isRemote) {
                    Chat.messageSP(Chat.Type.NOTIFY, player, new TextComponentTranslation("hardcorealchemy.instinct.homesick.nature.at_home"));
                }
                acknowledgedFeelingAtHome = true;
            }
        }
        if (feelsAtHome != feltAtHome) {
            timerHysteresisMark = timer;
        }
        feltAtHome = feelsAtHome;
        
        if (--directionTimer <= 0) {
            setRandomDirection(player);
        }
    }
    
    private static final float DEGREES_TO_RADIANS = (float)Math.PI / 180.0F;
    
    @Override
    public void aboutToMove(EntityPlayer player, EventPlayerMoveWithHeading event) {
        // Restrict player to only move along an axis
        // TODO: Don't forget to update your networking AND NBT!
        
        double forward = event.moveForward;
        // Attenuate strafing more to encourage the player to turn
        double strafe = event.moveStrafing*0.5;
        
        double directionX = MathHelper.sin(directionAngle * DEGREES_TO_RADIANS);
        double directionZ = MathHelper.cos(directionAngle * DEGREES_TO_RADIANS);
        double desiredForwardX = MathHelper.sin(player.rotationYaw * DEGREES_TO_RADIANS);
        double desiredForwardZ = MathHelper.cos(player.rotationYaw * DEGREES_TO_RADIANS);
        double desiredStrafeX = -desiredForwardZ;
        double desiredStrafeZ = desiredForwardX;
        double directionForward = desiredForwardX*directionX + desiredForwardZ*directionZ;
        double directionStrafe = desiredStrafeX*directionX + desiredStrafeZ*directionZ;
        double speed = MathHelper.sqrt(forward*forward*directionForward*directionForward + strafe*strafe*directionStrafe*directionStrafe);
        double direction = (forward*directionForward + strafe*directionStrafe) > 0.0D ? 1.0D : -1.0D;
        
        // Partially override forward/strafing.
        // Gives the sense of loss of control, while still allowing the player to move elsewhere if necessary
        //float overrideFraction = 0.8F;
        float overrideFraction = 0.5F + 0.5F*MathHelper.sin((float)(timer / 100) * (float)Math.PI);
        float originalFraction = 1.0F - overrideFraction;
        event.moveForward = originalFraction*(float)forward + overrideFraction*(float)(directionForward * speed * direction);
        event.moveStrafing = originalFraction*(float)strafe + overrideFraction*(float)(directionStrafe * speed * direction);
    }
}
