package slimeknights.tconstruct.library.tools.part;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents an item that has a Material associated with it. The NBT of the itemstack identifies which material the
 * itemstack of this item has.
 */
public class MaterialItem extends Item implements IMaterialItem {
  private static final String ADDED_BY = TConstruct.makeTranslationKey("tooltip", "part.added_by");

  public MaterialItem(Properties properties, @Nullable ResourceKey<CreativeModeTab> tab) {
    super(properties);
    if (tab != null)
      ItemGroupEvents.modifyEntriesEvent(tab).register(this::fillItemCategory);
  }

  /** Gets the material ID for the given NBT compound */
  private static MaterialVariantId getMaterialId(@Nullable CompoundTag nbt) {
    if (nbt != null) {
      String str = nbt.getString(MATERIAL_TAG);
      if (!str.isEmpty()) {
        MaterialVariantId id = MaterialVariantId.tryParse(str);
        if (id != null) {
          return id;
        }
      }
    }
    return IMaterial.UNKNOWN_ID;
  }

  @Override
  public MaterialVariantId getMaterial(ItemStack stack) {
    return getMaterialId(stack.getTag());
  }

  public void fillItemCategory(FabricItemGroupEntries items) {
    if (MaterialRegistry.isFullyLoaded()) {
      // if a specific material is set in the config, try adding that
      String showOnlyId = Config.COMMON.showOnlyPartMaterial.get();
      boolean added = false;
      if (!showOnlyId.isEmpty()) {
        MaterialVariantId materialId = MaterialVariantId.tryParse(showOnlyId);
        if (materialId != null && canUseMaterial(materialId.getId())) {
          items.accept(this.withMaterialForDisplay(materialId));
          added = true;
        }
      }
      // if no material is set or we failed to find it, iterate all materials
      if (!added) {
        for (IMaterial material : MaterialRegistry.getInstance().getVisibleMaterials()) {
          MaterialId id = material.getIdentifier();
          if (this.canUseMaterial(id)) {
            items.accept(this.withMaterial(id));
            // if a specific material was requested and not found, stop after first
            if (!showOnlyId.isEmpty()) {
              break;
            }
          }
        }
      }
    }
  }

  @Nullable
  private static Component getName(String baseKey, MaterialVariantId material) {
    // if there is a specific name, use that
    ResourceLocation location = material.getLocation('.');
    String fullKey = String.format("%s.%s.%s", baseKey, location.getNamespace(), location.getPath());
    if (Util.canTranslate(fullKey)) {
      return Component.translatable(fullKey);
    }
    // try material name prefix next
    String materialKey = MaterialTooltipCache.getKey(material);
    String materialPrefix = materialKey + ".format";
    if (Util.canTranslate(materialPrefix)) {
      return Component.translatable(materialPrefix, Component.translatable(baseKey));
    }
    // format as "<material> <item name>"
    if (Util.canTranslate(materialKey)) {
      return Component.translatable(TooltipUtil.KEY_FORMAT, Component.translatable(materialKey), Component.translatable(baseKey));
    }
    return null;
  }

  @Override
  public Component getName(ItemStack stack) {
    // if no material, return part name directly
    MaterialVariantId material = getMaterial(stack);
    if (material.equals(IMaterial.UNKNOWN_ID)) {
      return super.getName(stack);
    }
    // try variant first
    String key = this.getDescriptionId(stack);
    if (material.hasVariant()) {
      Component component = getName(key, material);
      if (component != null) {
        return component;
      }
    }
    // if variant did not work, do base material
    Component component = getName(key, material.getId());
    if (component != null) {
      return component;
    }
    // if neither worked, format directly
    return Component.translatable(key);
  }

  @Nullable
  @Override
  public String getCreatorModId(ItemStack stack) {
    MaterialVariantId material = getMaterial(stack);
    if (!IMaterial.UNKNOWN_ID.equals(material)) {
      return material.getId().getNamespace();
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
    return id == null ? null : id.getNamespace();
  }


  /**
   * Adds the mod that added the material to the tooltip
   * @param tooltip   Tooltip list
   * @param material  Material to add
   */
  protected static void addModTooltip(IMaterial material, List<Component> tooltip) {
    if (material != IMaterial.UNKNOWN) {
      tooltip.add(Component.empty());
      tooltip.add(Component.translatable(ADDED_BY, DomainDisplayName.nameFor(material.getIdentifier().getNamespace())));
    }
  }

  @Override
  public void verifyTagAfterLoad(CompoundTag nbt) {
    // if the material exists and was changed, update it
    MaterialVariantId id = getMaterialId(nbt);
    if (!id.equals(IMaterial.UNKNOWN_ID)) {
      MaterialId original = id.getId();
      MaterialId resolved = MaterialRegistry.getInstance().resolve(original);
      if (original != resolved) {
        nbt.putString(MATERIAL_TAG, MaterialVariantId.create(resolved, id.getVariant()).toString());
      }
    }
  }
}
