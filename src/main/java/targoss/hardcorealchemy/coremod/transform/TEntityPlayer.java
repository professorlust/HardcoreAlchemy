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

package targoss.hardcorealchemy.coremod.transform;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import targoss.hardcorealchemy.coremod.MethodPatcher;
import targoss.hardcorealchemy.coremod.ObfuscatedName;

public class TEntityPlayer extends MethodPatcher {
    private static final String ENTITY_PLAYER = "net.minecraft.entity.player.EntityPlayer";
    private static final ObfuscatedName MOVE_ENTITY_WITH_HEADING = new ObfuscatedName("func_70612_e" /*moveEntityWithHeading*/);
    private static final String EVENT_PLAYER_MOVE_WITH_HEADING = "targoss/hardcorealchemy/event/EventPlayerMoveWithHeading";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals(ENTITY_PLAYER)) {
            return transformClass(transformedName, basicClass, ClassWriter.COMPUTE_MAXS);
        }
        return basicClass;
    }

    @Override
    public void transformMethod(MethodNode method) {
        if (method.name.equals(MOVE_ENTITY_WITH_HEADING.get())) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0)); // EntityPlayer
            hook.add(new VarInsnNode(Opcodes.FLOAD, 1)); // float moveStrafing
            hook.add(new VarInsnNode(Opcodes.FLOAD, 2)); // float moveForward
            // Post new EventPlayerMoveWithHeading
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    EVENT_PLAYER_MOVE_WITH_HEADING,
                    "onPlayerMoveWithHeading",
                    "(L" + ENTITY_PLAYER.replace(".", "/") + ";FF)L" + EVENT_PLAYER_MOVE_WITH_HEADING + ";",
                    false));
            hook.add(new InsnNode(Opcodes.DUP));
            // There are now two references to the processed event which we will use to store new movement state
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, EVENT_PLAYER_MOVE_WITH_HEADING, "moveStrafing", "F"));
            hook.add(new VarInsnNode(Opcodes.FSTORE, 1));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, EVENT_PLAYER_MOVE_WITH_HEADING, "moveForward", "F"));
            hook.add(new VarInsnNode(Opcodes.FSTORE, 2));
            
            method.instructions.insert(hook);
        }
    }

}
