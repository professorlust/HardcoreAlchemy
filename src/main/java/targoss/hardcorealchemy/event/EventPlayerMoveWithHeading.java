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

package targoss.hardcorealchemy.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import targoss.hardcorealchemy.coremod.CoremodHook;

/**
 * Event called directly before player movement is updated.
 * Do not set player.moveStrafing or player.moveForward; those
 * will be set automatically by this event.
 */
public class EventPlayerMoveWithHeading extends Event {
    public final EntityPlayer player;
    public float moveStrafing;
    public float moveForward;
    
    public EventPlayerMoveWithHeading(EntityPlayer player, float moveStrafing, float moveForward) {
        this.player = player;
        this.moveStrafing = moveStrafing;
        this.moveForward = moveForward;
    }
    
    @CoremodHook
    public static EventPlayerMoveWithHeading onPlayerMoveWithHeading(EntityPlayer player, float moveStrafing, float moveForward) {
        EventPlayerMoveWithHeading event = new EventPlayerMoveWithHeading(player, moveStrafing, moveForward);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }
}
