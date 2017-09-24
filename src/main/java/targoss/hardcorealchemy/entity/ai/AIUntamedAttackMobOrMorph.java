package targoss.hardcorealchemy.entity.ai;

import com.google.common.base.Predicate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.passive.EntityTameable;

/**
 * Very similar to EntityAITargetNonTamed, but its superclass is changed so the mob respects morphs
 */
public class AIUntamedAttackMobOrMorph<T extends EntityLivingBase> extends AIAttackTargetMobOrMorph<T> {
    public final EntityTameable theTameable;

    public AIUntamedAttackMobOrMorph(EntityAITargetNonTamed<T> aiIgnoringMorph)
    {
        super(aiIgnoringMorph);
        this.theTameable = aiIgnoringMorph.theTameable;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        return !this.theTameable.isTamed() && super.shouldExecute();
    }
}