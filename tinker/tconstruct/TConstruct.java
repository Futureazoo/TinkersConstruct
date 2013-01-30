package tinker.tconstruct;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;
import tinker.tconstruct.client.gui.ToolGuiElement;
import tinker.tconstruct.crafting.PatternBuilder;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;

/** TConstruct, the tool mod.
 * Craft your tools with style, then modify until the original is gone!
 * @author: mDiyo
 */

@Mod(modid = "TConstruct", name = "TConstruct", version = "mc1.4.7_1.0.7")
@NetworkMod(serverSideRequired = false, clientSideRequired = true, channels={"TConstruct"}, packetHandler = tinker.tconstruct.TConstructPacketHandler.class)
public class TConstruct 
{
	/* Instance of this mod, used for grabbing prototype fields */
	@Instance("TConstruct")
	public static TConstruct instance;
	/* Proxies for sides, used for graphics processing */
	@SidedProxy(clientSide = "tinker.tconstruct.client.TProxyClient", serverSide = "tinker.tconstruct.TProxyCommon")
	public static TProxyCommon proxy;
	
	public static ArrayList<ToolGuiElement> toolButtons = new ArrayList<ToolGuiElement>(20);
	
	@PreInit
	public void preInit(FMLPreInitializationEvent evt)
	{
		MinecraftForge.EVENT_BUS.register(this);
		PHConstruct.initProps();
		materialTab = new TabTools("TConstructMaterials");
		toolTab = new TabTools("TConstructTools");
		blockTab = new TabTools("TConstructBlocks");
		content = new TConstructContent();
		
		NetworkRegistry.instance().registerGuiHandler(instance, new TConstructGuiHandler());
		addToolButtons();
	}
	
	@Init
	public void load(FMLInitializationEvent evt) 
	{
		proxy.registerRenderer();
		proxy.addNames();
		//GameRegistry.registerWorldGenerator(new TBaseWorldGenerator());
	}
	
	@PostInit
	public void postInit(FMLPostInitializationEvent evt) 
	{
		ArrayList<ItemStack> copperIngots = OreDictionary.getOres("ingotCopper");
		for (ItemStack copper : copperIngots)
			PatternBuilder.instance.registerMaterial(copper, 2, "copper");
		
		ArrayList<ItemStack> bronzeIngots = OreDictionary.getOres("ingotBronze");
		for (ItemStack bronze : bronzeIngots)
			PatternBuilder.instance.registerMaterial(bronze, 2, "bronze");
	}
	
	@ForgeSubscribe
    public void registerOre(OreRegisterEvent evt)
	{
		if (evt.Name == "battery")
			content.modE.batteries.add(evt.Ore);
		
		if (evt.Name == "basicCircuit")
			content.modE.circuits.add(evt.Ore);
		
		if (evt.Name == "ingotCopper")
			PatternBuilder.instance.registerMaterial(evt.Ore, 2, "copper");
		
		if (evt.Name == "ingotBronze")
			PatternBuilder.instance.registerMaterial(evt.Ore, 2, "bronze");
	}
	
	static int[][] slotTypes = {
		new int[] {0, 3, 0}, //Repair
		new int[] {1, 4, 0}, //Pickaxe
		new int[] {2, 5, 0}, //Shovel
		new int[] {2, 6, 0}, //Axe
		//new int[] {2, 9, 0}, //Lumber Axe
		//new int[] {1, 7, 0}, //Ice Axe
		new int[] {3, 8, 0}, //Mattock
		new int[] {1, 0, 1}, //Broadsword
		new int[] {1, 1, 1}, //Longsword
		new int[] {1, 2, 1}, //Rapier
		new int[] {2, 3, 1}, //Frying pan
		new int[] {2, 4, 1} //Battlesign
	};
	
	static int[][] iconCoords = {
		new int[] { 0, 1, 2 }, new int[] { 13, 13, 13 }, //Repair
		new int[] { 0, 0, 1 }, new int[] { 2, 3, 3 }, //Pickaxe
		new int[] { 3, 0, 13 }, new int[] { 2, 3, 13 }, //Shovel
		new int[] { 2, 0, 13 }, new int[] { 2, 3, 13 }, //Axe
		//new int[] { 6, 0, 13 }, new int[] { 2, 3, 13 }, //Lumber Axe
		//new int[] { 0, 0, 5 }, new int[] { 2, 3, 3 }, //Ice Axe
		new int[] { 2, 0, 3 }, new int[] { 2, 3, 2 }, //Mattock
		new int[] { 1, 0, 2 }, new int[] { 2, 3, 3 }, //Broadsword
		new int[] { 1, 0, 3 }, new int[] { 2, 3, 3 }, //Longsword
		new int[] { 1, 0, 4 }, new int[] { 2, 3, 3 }, //Rapier
		new int[] { 4, 0, 13 }, new int[] { 2, 3, 13 }, //Frying Pan
		new int[] { 5, 0, 13 }, new int[] { 2, 3, 13 } //Battlesign
	};
	
	static String[] toolNames = {
		"Repair and Modification",
		"Pickaxe",
		"Shovel",
		"Axe",
		//"Lumber Axe",
		//"Ice Axe",
		"Mattock",
		"Broadsword",
		"Longsword",
		"Rapier",
		"Frying Pan",
		"Battlesign"
	};
	
	static String[] toolDescriptions = {
		"The main way to repair or change your tools. Place a tool and a material on the left to get started.",
		"The Pickaxe is a basic mining tool. It is effective on stone and ores.\n\nRequired parts:\n- Pickaxe Head\n- Tool Binding\n- Handle",
		"The Shovel is a basic digging tool. It is effective on dirt, sand, and snow.\n\nRequired parts:\n- Shovel Head\n- Handle",
		"The Axe is a basic chopping tool. It is effective on wood and leaves.\n\nRequired parts:\n- Axe Head\n- Handle",
		//"The Lumber Axe is a broad chopping tool. It harvests wood in a wide range and can fell entire trees.\n\nRequired parts:\n- Broad Axe Head\n- Handle",
		//"The Ice Axe is a tool for harvesting ice, mining, and attacking foes.\n\nSpecial Ability:\n- Wall Climb\nNatural Ability:\n- Ice Harvest\nDamage: Moderate\n\nRequired parts:\n- Pickaxe Head\n- Spike\n- Handle",
		"The Cutter Mattock is a versatile farming tool. It is effective on wood, dirt, and plants.\n\nSpecial Ability: Hoe\n\nRequired parts:\n- Axe Head\n- Shovel Head\n- Handle",
		"The Broadsword is a defensive weapon. Blocking cuts damage in half.\n\nSpecial Ability: Block\nDamage: Moderate\nDurability: High\n\nRequired parts:\n- Sword Blade\n- Wide Guard\n- Handle",
		"The Longsword is a balanced weapon. It is useful for knocking enemies away or getting in and out of battle quickly.\n\nNatural Ability:\n- Charge Boost\nDamage: Moderate\nDurability: Moderate\n\nRequired parts:\n- Sword Blade\n- Hand Guard\n- Handle",
		"The Rapier is an offensive weapon that relies on quick strikes to defeat foes.\n\nNatural Abilities:\n- Armor Pierce\n- Quick Strike\n- Charge Boost\nDamage: High\nDurability: Low\n\nRequired parts:\n- Sword Blade\n- Crossbar\n- Handle",
		"The Frying is a heavy weapon that uses sheer weight to stun foes.\n\nSpecial Ability: Block\nNatural Ability: Bash\nShift+rClick: Place Frying Pan\nDamage: High\nDurability: High\n\nRequired parts:\n- Pan\n- Handle",
		//"The Battlesign is an advance in weapon technology worthy of Zombie Pigmen everywhere.\n\nSpecial Ability: Block\nShift-rClick: Place sign\nDamage: Low\nDurability: Average\n\nRequired parts:\n- Board\n- Handle"
		"The Battlesign is an advance in weapon technology worthy of Zombie Pigmen everywhere.\n\nSpecial Ability: Block\nDamage: Low\nDurability: Average\n\nRequired parts:\n- Sign Board\n- Handle"
	};
	
	void addToolButtons()
	{
		for (int i = 0; i < toolNames.length; i++)
		{
			addToolButton(slotTypes[i][0], slotTypes[i][1], slotTypes[i][2], iconCoords[i*2], iconCoords[i*2+1], toolNames[i], toolDescriptions[i]);
		}
	}
	
	private void addToolButton(int slotType, int xButton, int yButton, int[] xIcons, int[] yIcons, String title, String body)
	{
		toolButtons.add(new ToolGuiElement(slotType, xButton, yButton, xIcons, yIcons, title, body));
	}
	
	public static void addToolButton(int slotType, int xButton, int yButton, int[] xIcons, int[] yIcons, String title, String body, String texture)
	{
		toolButtons.add(new ToolGuiElement(slotType, xButton, yButton, xIcons, yIcons, title, body, texture));
	}
	
	TConstructContent content;
	
	public static Random tRand = new Random();
	public static TabTools toolTab;
	public static TabTools materialTab;
	public static TabTools blockTab;
}
