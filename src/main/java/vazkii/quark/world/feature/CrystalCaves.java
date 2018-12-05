package vazkii.quark.world.feature;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.arl.recipe.RecipeHandler;
import vazkii.arl.util.ProxyRegistry;
import vazkii.quark.base.handler.DimensionConfig;
import vazkii.quark.base.handler.ModIntegrationHandler;
import vazkii.quark.base.module.Feature;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.misc.feature.ColorRunes;
import vazkii.quark.world.block.BlockCrystal;
import vazkii.quark.world.world.CrystalCaveGenerator;

public class CrystalCaves extends Feature {

	public static Block crystal;
	
	DimensionConfig dims;
	public static int crystalCaveRarity;
	
	@Override
	public void setupConfig() {
		crystalCaveRarity = loadPropInt("Crystal Cave Rarity", "Given this value as X, crystal caves will spawn on average 1 per X chunks", 150);
		dims = new DimensionConfig(configCategory);
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent event) {
		crystal = new BlockCrystal();
		
		GameRegistry.registerWorldGenerator(new CrystalCaveGenerator(dims), 1);
		
		ModIntegrationHandler.allowChiselAndBitsChiseling(crystal);
	}
	
	@Override
	public void postPreInit(FMLPreInitializationEvent event) {		
		if(ModuleLoader.isFeatureEnabled(ColorRunes.class)) {
			addRuneRecipe(0, 0);
			addRuneRecipe(1, 14);
			addRuneRecipe(2, 1);
			addRuneRecipe(3, 4);
			addRuneRecipe(4, 5);
			addRuneRecipe(5, 3);
			addRuneRecipe(6, 11);
			addRuneRecipe(7, 2);
		}
	}
	
	private void addRuneRecipe(int crystalMeta, int runeMeta) {
		RecipeHandler.addOreDictRecipe(ProxyRegistry.newStack(ColorRunes.rune, 1, runeMeta), 
				"CCC", "CSC", "CCC",
				'C', ProxyRegistry.newStack(crystal, 1, crystalMeta),
				'S', "stone");
	}
	
	@Override
	public boolean requiresMinecraftRestartToEnable() {
		return true;
	}
	
}
