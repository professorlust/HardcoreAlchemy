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

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PingPong {
    /**
     * Expected network transit time from the client to the server and back (ms)
     * Currently only available on the client.
     */
    public static int transitTime;
    
    protected static int nextTransitTime = 0;
    protected static int[] transitTimes = new int[9];
    
    /**
     * Adds a time to a history and sets transitTime to the median of the history
     */
    protected static synchronized void recordTransitTime(int time) {
        transitTimes[nextTransitTime++] = time;
        if (nextTransitTime >= transitTimes.length) nextTransitTime = 0;
        int[] transitTimesSorted = Arrays.copyOf(transitTimes, transitTimes.length);
        Arrays.sort(transitTimesSorted);
        transitTime = transitTimesSorted[transitTimes.length / 2];
    }
    
    public static class Ping extends MessageToServer {
        
        public Ping() {
            time = System.currentTimeMillis();
        }
        
        public long time;

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(time);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            time = buf.readLong();
        }
        
        public static class Handler implements IMessageHandler<Ping, IMessage> {
            @Override
            public IMessage onMessage(Ping message, MessageContext ctx) {
                return new Pong(message.time);
            }
        }

        @Override
        public Class<? extends IMessageHandler<? extends MessageToServer, IMessage>> getHandlerClass() {
            return Handler.class;
        }
        
    }
    
    public static class Pong extends MessageToClient {
        public Pong() {}
        
        public Pong(long pingTime) {
            this.pingTime = pingTime;
        }
        
        public long pingTime;

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(pingTime);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            pingTime = buf.readLong();
        }
        
        public static class Handler implements IMessageHandler<Pong, IMessage> {
            @Override
            public IMessage onMessage(Pong message, MessageContext ctx) {
                long pongTime = System.currentTimeMillis();
                PingPong.recordTransitTime((int)(pongTime - message.pingTime));
                return null;
            }
        }

        @Override
        public Class<? extends IMessageHandler<? extends MessageToClient, IMessage>> getHandlerClass() {
            return Handler.class;
        }
        
    }
}
