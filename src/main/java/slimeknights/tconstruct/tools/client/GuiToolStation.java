package slimeknights.tconstruct.tools.client;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.io.IOException;
import java.util.List;

import slimeknights.tconstruct.common.TinkerNetwork;
import slimeknights.mantle.client.gui.GuiElement;
import slimeknights.mantle.client.gui.GuiModule;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tinkering.TinkersItem;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.tools.client.module.GuiButtonsToolStation;
import slimeknights.tconstruct.tools.inventory.ContainerToolStation;
import slimeknights.tconstruct.tools.network.ToolStationSelectionPacket;
import slimeknights.tconstruct.tools.network.ToolStationTextPacket;
import slimeknights.tconstruct.tools.tileentity.TileToolStation;
import slimeknights.mantle.client.gui.GuiElementScalable;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.tools.client.module.GuiInfoPanel;
import slimeknights.tconstruct.tools.inventory.ContainerTinkerStation;
import slimeknights.tconstruct.tools.inventory.SlotToolStationIn;

@SideOnly(Side.CLIENT)
public class GuiToolStation extends GuiTinkerStation {

  private static final ResourceLocation BACKGROUND = Util.getResource("textures/gui/toolstation.png");

  private static final GuiElement TextFieldActive = new GuiElement(0, 210, 102, 12, 256, 256);
  private static final GuiElement ItemCover = new GuiElement(176, 18, 80, 64);
  private static final GuiElement SlotBackground = new GuiElement(176, 0, 18, 18);
  private static final GuiElement SlotBorder = new GuiElement(194, 0, 18, 18);

  private static final GuiElement SlotSpaceTop = new GuiElement(0, 174 + 2, 18, 2);
  private static final GuiElement SlotSpaceBottom = new GuiElement(0, 174, 18, 2);
  private static final GuiElement PanelSpaceL = new GuiElement(0, 174, 5, 4);
  private static final GuiElement PanelSpaceR = new GuiElement(9, 174, 9, 4);

  private static final GuiElement BeamLeft = new GuiElement(0, 180, 2, 7);
  private static final GuiElement BeamRight = new GuiElement(131, 180, 2, 7);
  private static final GuiElementScalable BeamCenter = new GuiElementScalable(2, 180, 129, 7);

  public static final int Column_Count = 5;
  private static final int Table_slot_count = 6;

  protected GuiElement buttonDecorationTop = SlotSpaceTop;
  protected GuiElement buttonDecorationBot = SlotSpaceBottom;
  protected GuiElement panelDecorationL = PanelSpaceL;
  protected GuiElement panelDecorationR = PanelSpaceR;

  protected GuiElement beamL = new GuiElement(0, 0, 0, 0);
  protected GuiElement beamR = new GuiElement(0, 0, 0, 0);
  protected GuiElementScalable beamC = new GuiElementScalable(0, 0, 0, 0);

  protected GuiButtonsToolStation buttons;
  protected int activeSlots; // how many of the available slots are active

  public GuiTextField textField;

  protected GuiInfoPanel toolInfo;
  protected GuiInfoPanel traitInfo;

  public ToolBuildGuiInfo currentInfo = GuiButtonRepair.info;


  public GuiToolStation(InventoryPlayer playerInv, World world, BlockPos pos, TileToolStation tile) {
    super(world, pos, (ContainerTinkerStation) tile.createContainer(playerInv, world, pos));

    buttons = new GuiButtonsToolStation(this, inventorySlots);
    this.addModule(buttons);
    toolInfo = new GuiInfoPanel(this, inventorySlots);
    this.addModule(toolInfo);
    traitInfo = new GuiInfoPanel(this, inventorySlots);
    this.addModule(traitInfo);

    toolInfo.yOffset = 5;
    traitInfo.yOffset = toolInfo.getYSize() + 9;

    this.ySize = 174;

    wood();
  }

  @Override
  public void initGui() {
    super.initGui();
    Keyboard.enableRepeatEvents(true);

    // workaround to line up the tabs on switching even though the GUI is a tad higher
    this.guiTop += 4;
    this.cornerY += 4;

    // todo: sync text via network
    textField = new GuiTextField(0, fontRendererObj, cornerX + 70, cornerY + 7, 92, 12);
    //textField.setFocused(true);
    //textField.setCanLoseFocus(false);
    textField.setEnableBackgroundDrawing(false);
    textField.setMaxStringLength(40);

    buttons.xOffset = -2;
    buttons.yOffset = beamC.h + buttonDecorationTop.h;
    toolInfo.xOffset = 2;
    toolInfo.yOffset = beamC.h + panelDecorationL.h;
    traitInfo.xOffset = toolInfo.xOffset;
    traitInfo.yOffset = toolInfo.yOffset + toolInfo.getYSize() + 4;

    for(GuiModule module : modules) {
      module.guiTop += 4;
    }

    updateGUI();
  }

  @Override
  public void onGuiClosed() {
    super.onGuiClosed();
    Keyboard.enableRepeatEvents(false);
  }

  public void onToolSelection(ToolBuildGuiInfo info) {
    activeSlots = Math.min(info.positions.size(), Table_slot_count);
    currentInfo = info;

    ToolCore tool = null;

    if(info.tool != null && info.tool.getItem() instanceof ToolCore) {
      tool = (ToolCore) info.tool.getItem();
    }

    ((ContainerToolStation) inventorySlots).setToolSelection(tool, activeSlots);
    // update the server (and others)
    TinkerNetwork.sendToServer(new ToolStationSelectionPacket(tool, activeSlots));
    updateGUI();
  }

  public void onToolSelectionPacket(ToolStationSelectionPacket packet) {
    ToolBuildGuiInfo info = TinkerRegistryClient.getToolBuildInfoForTool(packet.tool);
    if(info == null) {
      info = GuiButtonRepair.info;
    }
    activeSlots = packet.activeSlots;
    currentInfo = info;

    buttons.setSelectedbuttonByTool(currentInfo.tool);

    updateGUI();
  }

  public void updateGUI() {
    int i;
    for(i = 0; i < activeSlots; i++) {
      Point point = currentInfo.positions.get(i);

      Slot slot = inventorySlots.getSlot(i);
      slot.xDisplayPosition = point.getX();
      slot.yDisplayPosition = point.getY();
    }

    // remaining slots
    int stillFilled = 0;
    for(; i < Table_slot_count; i++) {
      Slot slot = inventorySlots.getSlot(i);

      if(slot.getHasStack()) {
        slot.xDisplayPosition = 87 + 20 * stillFilled;
        slot.yDisplayPosition = 62;
        stillFilled++;
      }
      else {
        // todo: slot.disable
        slot.xDisplayPosition = 0;
        slot.yDisplayPosition = 0;
      }
    }

    updateDisplay();
  }

  @Override
  public void updateDisplay() {
    // tool info of existing or tool to build
    ContainerToolStation container = (ContainerToolStation) inventorySlots;
    ItemStack toolStack = container.getResult();
    if(toolStack == null) toolStack = inventorySlots.getSlot(0).getStack();

    // current tool to build or repair/modify
    if(toolStack != null && toolStack.getItem() instanceof ToolCore) {
      ToolCore tool = (ToolCore) toolStack.getItem();
      toolInfo.setCaption(tool.getLocalizedToolName());
      toolInfo.setText(tool.getInformation(toolStack));

      traitInfo.setCaption(StatCollector.translateToLocal("gui.toolStation.traits"));

      List<String> mods = Lists.newLinkedList();
      List<String> tips = Lists.newLinkedList();
      NBTTagList tagList = TagUtil.getModifiersTagList(toolStack);
      for(int i = 0; i < tagList.tagCount(); i++) {
        NBTTagCompound tag = tagList.getCompoundTagAt(i);
        ModifierNBT data = ModifierNBT.readTag(tag);

        // get matching modifier
        IModifier modifier = TinkerRegistry.getModifier(data.identifier);
        if(modifier == null) {
          continue;
        }

        mods.add(data.color + modifier.getTooltip(tag, true));
        tips.add(modifier.getLocalizedDesc());
      }

      if(mods.isEmpty()) {
        mods.add(StatCollector.translateToLocal("gui.toolStation.noTraits"));
      }

      traitInfo.setText(mods);
      traitInfo.setTooltips(tips);
    }
    // repair info
    else if(currentInfo.tool == null) {
      toolInfo.setCaption(StatCollector.translateToLocal("gui.toolStation.repair"));
      toolInfo.setText();

      traitInfo.setCaption(null);
      String c = EnumChatFormatting.DARK_GRAY.toString();
      String[] art = new String[] {
          c + "",
          c + "",
          c + "       .",
          c + "     /( _________",
          c + "     |  >:=========`",
          c + "     )(  ",
          c + "     \"\""
      };
      traitInfo.setText(art);
    }
    // tool build info
    else {
      ToolCore tool = (ToolCore)currentInfo.tool.getItem();
      toolInfo.setCaption(tool.getLocalizedToolName());
      toolInfo.setText(tool.getLocalizedDescription());

      // Components
      List<String> text = Lists.newLinkedList();
      for(int i = 0; i < tool.requiredComponents.length; i++) {
        PartMaterialType pmt = tool.requiredComponents[i];
        StringBuilder sb = new StringBuilder();

        ItemStack slotStack = container.getSlot(i).getStack();
        if(!pmt.isValid(slotStack)) {
          sb.append(EnumChatFormatting.RED);

          // is an item in the slot?
          if(slotStack != null && slotStack.getItem() instanceof IToolPart) {
            if(pmt.isValidItem((IToolPart) slotStack.getItem())) {
              // the item has an invalid material
              warning(Util.translate("gui.error.wrongMaterialPart"));
            }
          }
        }

        sb.append(" * ");
        for(IToolPart part : pmt.getPossibleParts()) {
          if(part instanceof Item) {
            sb.append(((Item) part).getItemStackDisplayName(new ItemStack((Item) part)));
            sb.append("/");
          }
        }
        sb.deleteCharAt(sb.length()-1); // removes last '/'
        text.add(sb.toString());
      }
      traitInfo.setCaption(StatCollector.translateToLocal("gui.toolStation.components"));
      traitInfo.setText(text.toArray(new String[text.size()]));
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    textField.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if(!textField.isFocused()) {
      super.keyTyped(typedChar, keyCode);
    }
    else {
      if(keyCode == 1) {
        this.mc.thePlayer.closeScreen();
      }

      textField.textboxKeyTyped(typedChar, keyCode);
      TinkerNetwork.sendToServer(new ToolStationTextPacket(textField.getText()));
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    textField.updateCursorCounter();
  }

  @Override
  public void drawSlot(Slot slotIn) {
    // don't draw dormant slots with no item
    if(slotIn instanceof SlotToolStationIn && ((SlotToolStationIn) slotIn).isDormant() && !slotIn.getHasStack())
      return;

    super.drawSlot(slotIn);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    drawBackground(BACKGROUND);

    if(textField.isFocused()) {
      TextFieldActive.draw(cornerX + 68, cornerY + 6);
    }

    // draw textfield
    textField.drawTextBox();

    int xOff = 0;
    int yOff = 0;

    int x = 0;
    int y = 0;

    // draw the item background
    final float scale = 4.0f;
    GlStateManager.scale(scale, scale, 1.0f);
    {
      int logoX = (this.cornerX + 10) / 4 + xOff;
      int logoY = (this.cornerY + 18) / 4 + yOff;

      if(currentInfo != null) {
        if(currentInfo.tool != null) {
          itemRender.renderItemIntoGUI(currentInfo.tool, logoX, logoY);
        }
        else if(currentInfo == GuiButtonRepair.info) {
          this.mc.getTextureManager().bindTexture(ICONS);
          ICON_Anvil.draw(logoX, logoY);
        }
      }
    }
    GlStateManager.scale(1f / scale, 1f / scale, 1.0f);

    // rebind gui texture since itemstack drawing sets it to something else
    this.mc.getTextureManager().bindTexture(BACKGROUND);

    // reset state after item drawing
    GlStateManager.enableBlend();
    GlStateManager.enableAlpha();
    RenderHelper.disableStandardItemLighting();
    GlStateManager.disableDepth();

    // draw the halftransparent "cover" over the item
    GlStateManager.color(1.0f, 1.0f, 1.0f, 0.82f);
    ItemCover.draw(this.cornerX + 7, this.cornerY + 18);

    // the slot backgrounds
    GlStateManager.color(1.0f, 1.0f, 1.0f, 0.28f);
    for(int i = 0; i < activeSlots; i++) {
      Slot slot = inventorySlots.getSlot(i);
      SlotBackground.draw(x + this.cornerX + slot.xDisplayPosition - 1, y + this.cornerY + slot.yDisplayPosition - 1);
    }

    // full opaque. Draw the borders of the slots
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    for(int i = 0; i < Table_slot_count; i++) {
      Slot slot = inventorySlots.getSlot(i);
      if(slot instanceof SlotToolStationIn && (!((SlotToolStationIn) slot).isDormant() || slot.getHasStack())) {
        SlotBorder.draw(
            x + this.cornerX + slot.xDisplayPosition - 1, y + this.cornerY + slot.yDisplayPosition - 1);
      }
    }

    this.mc.getTextureManager().bindTexture(ICONS);

    // slot logos
    if(currentInfo == GuiButtonRepair.info) {
      drawRepairSlotIcons();
    }
    else if(currentInfo.tool != null && currentInfo.tool.getItem() instanceof TinkersItem) {
      for(int i = 0; i < activeSlots; i++) {
        Slot slot = inventorySlots.getSlot(i);
        if(!(slot instanceof SlotToolStationIn)) {
          continue;
        }

        ItemStack stack = ((SlotToolStationIn) slot).icon;
        if(stack == null) {
          continue;
        }

        itemRender.renderItemIntoGUI(stack,
                                     x + this.cornerX + slot.xDisplayPosition,
                                     y + this.cornerY + slot.yDisplayPosition);
      }
    }


    this.mc.getTextureManager().bindTexture(BACKGROUND);
    x = buttons.guiLeft - beamL.w;
    y = cornerY;
    // draw the beams at the top
    x += beamL.draw(x, y);
    x += beamC.drawScaledX(x, y, buttons.xSize);
    beamR.draw(x, y);

    x = toolInfo.guiLeft - beamL.w;
    x += beamL.draw(x, y);
    x += beamC.drawScaledX(x, y, toolInfo.xSize);
    beamR.draw(x, y);

    // draw the decoration for the buttons
    for(Object o : buttons.buttonList) {
      GuiButton button = (GuiButton) o;

      buttonDecorationTop.draw(button.xPosition, button.yPosition - buttonDecorationTop.h);
      // don't draw the bottom for the buttons in the last row
      if(button.id < buttons.buttonList.size() - Column_Count) {
        buttonDecorationBot.draw(button.xPosition, button.yPosition + button.height);
      }
    }

    // draw the decorations for the panels
    panelDecorationL.draw(toolInfo.guiLeft + 5, toolInfo.guiTop - panelDecorationL.h);
    panelDecorationR.draw(toolInfo.guiRight() - 5 - panelDecorationR.w, toolInfo.guiTop - panelDecorationR.h);
    panelDecorationL.draw(traitInfo.guiLeft + 5, traitInfo.guiTop - panelDecorationL.h);
    panelDecorationR.draw(traitInfo.guiRight() - 5 - panelDecorationR.w, traitInfo.guiTop - panelDecorationR.h);

    GlStateManager.enableDepth();

    // continue as usual and hope that the drawing state is not completely wrecked
    super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
  }

  protected void drawRepairSlotIcons() {
    for(int i = 0; i < activeSlots; i++) {
      drawRepairSlotIcon(i);
    }
  }

  protected void drawRepairSlotIcon(int i) {
    GuiElement icon = null;
    Slot slot = inventorySlots.getSlot(i);
    // only empty solts get the logo since something else than the displayed thing might be in there.
    // which would look weird.
    if(slot.getHasStack()) {
      return;
    }

    if(i == 0) {
      icon = ICON_Pickaxe;
    }
    else if(i == 1) {
      icon = ICON_Dust;
    }
    else if(i == 2) {
      icon = ICON_Lapis;
    }
    else if(i == 3) {
      icon = ICON_Ingot;
    }
    else if(i == 4) {
      icon = ICON_Gem;
    }
    else if(i == 5) {
      icon = ICON_Quartz;
    }

    if(icon != null) {
      drawIconEmpty(slot, icon);
    }
  }

  protected void wood() {
    toolInfo.wood();
    traitInfo.wood();

    buttonDecorationTop = SlotSpaceTop.shift(SlotSpaceTop.w, 0);
    buttonDecorationBot = SlotSpaceBottom.shift(SlotSpaceBottom.w, 0);
    panelDecorationL = PanelSpaceL.shift(18, 0);
    panelDecorationR = PanelSpaceR.shift(18, 0);

    buttons.wood();

    beamL = BeamLeft;
    beamR = BeamRight;
    beamC = BeamCenter;
  }

  protected void metal() {
    toolInfo.metal();
    traitInfo.metal();

    buttonDecorationTop = SlotSpaceTop.shift(SlotSpaceTop.w * 2, 0);
    buttonDecorationBot = SlotSpaceBottom.shift(SlotSpaceBottom.w * 2, 0);
    panelDecorationL = PanelSpaceL.shift(18 * 2, 0);
    panelDecorationR = PanelSpaceR.shift(18 * 2, 0);

    buttons.metal();

    beamL = BeamLeft.shift(0, BeamLeft.h);
    beamR = BeamRight.shift(0, BeamRight.h);
    beamC = BeamCenter.shift(0, BeamCenter.h);
  }

  @Override
  public void error(String message) {
    toolInfo.setCaption(StatCollector.translateToLocal("gui.error"));
    toolInfo.setText(message);
    traitInfo.setCaption(null);
    traitInfo.setText();
  }

  @Override
  public void warning(String message) {
    toolInfo.setCaption(StatCollector.translateToLocal("gui.warning"));
    toolInfo.setText(message);
    traitInfo.setCaption(null);
    traitInfo.setText();
  }
}