package tconstruct.tools.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import tconstruct.library.Util;
import tconstruct.tools.inventory.ContainerMultiModule;
import tconstruct.tools.tileentity.TileStencilTable;

@SideOnly(Side.CLIENT)
public class GuiStencilTable extends GuiTinkerStation {
  private static final ResourceLocation BACKGROUND = Util.getResource("textures/gui/stenciltable.png");

  public GuiStencilTable(InventoryPlayer playerInv, World world, BlockPos pos, TileStencilTable tile) {
    super(world, pos, (ContainerMultiModule)tile.createContainer(playerInv, world, pos));

    this.xSize = 194;
    this.ySize = 168;
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.getTextureManager().bindTexture(BACKGROUND);
    this.drawTexturedModalRect(cornerX, cornerY, 0, 0, realWidth, realHeight);

    super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
  }
}