package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.shared.TinkerDamageTypes;
import slimeknights.tconstruct.tools.modifiers.traits.melee.LaceratingModifier;

/**
 * Potion effect from {@link LaceratingModifier}
 */
public class BleedingEffect extends NoMilkEffect {
  private static final String SOURCE_KEY = TConstruct.prefix("bleed");
  public BleedingEffect() {
    super(MobEffectCategory.HARMFUL, 0xa80000, true);
  }

  @Override
  public boolean isDurationEffectTick(int tick, int level) {
    // every half second
    return tick > 0 && tick % 20 == 0;
  }

  @Override
  public void applyEffectTick(LivingEntity target, int level) {
    // attribute to player kill
    LivingEntity lastAttacker = target.getLastHurtMob();
    DamageSource source;
    if(lastAttacker != null) {
      source = new BleedingDamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TinkerDamageTypes.BLEEDING), lastAttacker);
    }
    else {
      source = new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TinkerDamageTypes.BLEEDING));
    }

    // perform damage
    int hurtResistantTime = target.invulnerableTime;
    ToolAttackUtil.attackEntitySecondary(source, (level + 1f) / 2f, target, target, true);
    target.invulnerableTime = hurtResistantTime;

    // damage particles
    if (target.level() instanceof ServerLevel serverLevel) {
      serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), 1, 0.1, 0, 0.1, 0.2);
    }
  }

  /** Guardians use the direct entity to determine if they should thorns, while the direct marks for player kills
   * treat this as indirect damage by making the direct entity null, so guardians treat it like arrows */
  private static class BleedingDamageSource extends DamageSource {
    public BleedingDamageSource(Holder<DamageType> holder, Entity entity) {
      super(holder, entity);
    }

    @Nullable
    @Override
    public Entity getDirectEntity() {
      return null;
    }
  }
}
