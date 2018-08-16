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

package targoss.hardcorealchemy.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import targoss.hardcorealchemy.capability.instincts.ICapabilityInstinct;
import targoss.hardcorealchemy.capability.instincts.ICapabilityInstinct.InstinctEntry;
import targoss.hardcorealchemy.capability.instincts.ProviderInstinct;
import targoss.hardcorealchemy.instinct.IInstinct;
import targoss.hardcorealchemy.instinct.InstinctHomesickNature;
import targoss.hardcorealchemy.instinct.Instincts;
import targoss.hardcorealchemy.util.MiscVanilla;

public class MessageInstinctHomesickNature extends MessageToClient implements Runnable {
    public MessageInstinctHomesickNature() {}
    
    public MessageInstinctHomesickNature(InstinctHomesickNature homesickNature) {
        lastSeed = homesickNature.lastSeed;
        timer = homesickNature.timer;
        directionTimer = homesickNature.directionTimer;
        directionAngle = homesickNature.directionAngle;
    }

    public long lastSeed;
    public int timer;
    public int directionTimer;
    public float directionAngle;

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(lastSeed);
        buf.writeInt(timer);
        buf.writeInt(directionTimer);
        buf.writeFloat(directionAngle);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        lastSeed = buf.readLong();
        timer = buf.readInt();
        directionTimer = buf.readInt();
        directionAngle = buf.readFloat();
    }

    @Override
    public void run() {
        ICapabilityInstinct instinct = MiscVanilla.getTheMinecraftPlayer().getCapability(ProviderInstinct.INSTINCT_CAPABILITY, null);
        if (instinct == null) {
            return;
        }
        
        InstinctEntry activeInstinct = instinct.getInstinctMap().get(Instincts.HOMESICK_NATURE.getRegistryName());
        if (activeInstinct == null) {
            return;
        }
        
        InstinctHomesickNature homesickNature = (InstinctHomesickNature)activeInstinct.instinct;
        homesickNature.random.setSeed(lastSeed);
        homesickNature.lastSeed = lastSeed;
        // Compensate for delay between server processing and server receiving client reaction
        homesickNature.timer = timer + (PingPong.transitTime / 20 * (homesickNature.feltAtHome ? -1 : 1));
        homesickNature.directionTimer = directionTimer - PingPong.transitTime;
        homesickNature.directionAngle = directionAngle;
    }
    
    public static class Handler implements IMessageHandler<MessageInstinctHomesickNature, IMessage> {
        @Override
        public IMessage onMessage(MessageInstinctHomesickNature message, MessageContext ctx) {
            message.getThreadListener().addScheduledTask(message);
            return null;
        }
    }

    @Override
    public Class<? extends IMessageHandler<? extends MessageToClient, IMessage>> getHandlerClass() {
        return Handler.class;
    }

}
