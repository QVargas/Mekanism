package mekanism.client.gui;

import java.util.Arrays;
import mekanism.api.TileNetworkList;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.button.DisableableImageButton;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.item.ItemCraftingFormula;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.TileEntityFormulaicAssemblicator;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.BooleanStateDisplay.OnOff;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.TextComponentUtil;
import mekanism.common.util.text.Translation;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiFormulaicAssemblicator extends GuiMekanismTile<TileEntityFormulaicAssemblicator, FormulaicAssemblicatorContainer> {

    private Button encodeFormulaButton;
    private Button stockControlButton;
    private Button fillEmptyButton;
    private Button craftSingleButton;
    private Button craftAvailableButton;
    private Button autoModeButton;

    public GuiFormulaicAssemblicator(FormulaicAssemblicatorContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        ySize += 64;
    }

    @Override
    public void init() {
        super.init();
        ResourceLocation resource = getGuiLocation();
        addButton(new GuiSecurityTab<>(this, tileEntity, resource));
        addButton(new GuiUpgradeTab(this, tileEntity, resource));
        addButton(new GuiRedstoneControl(this, tileEntity, resource));
        addButton(new GuiSideConfigurationTab(this, tileEntity, resource));
        addButton(new GuiTransporterConfigTab(this, tileEntity, resource));
        addButton(new GuiVerticalPowerBar(this, tileEntity, resource, 159, 15));
        addButton(new GuiEnergyInfo(() -> Arrays.asList(
              TextComponentUtil.build(Translation.of("gui.mekanism.using"), ": ", EnergyDisplay.of(tileEntity.getEnergyPerTick()), "/t"),
              TextComponentUtil.build(Translation.of("gui.mekanism.needed"), ": ", EnergyDisplay.of(tileEntity.getNeededEnergy()))
        ), this, resource));
        addButton(new GuiSlot(SlotType.POWER, this, resource, 151, 75).with(SlotOverlay.POWER));

        addButton(encodeFormulaButton = new DisableableImageButton(guiLeft + 7, guiTop + 45, 14, 14, 176, 14, -14, 14, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(1))),
              getOnHover("gui.mekanism.encodeFormula")));
        addButton(stockControlButton = new DisableableImageButton(guiLeft + 26, guiTop + 75, 16, 16, 238, 48 + 16, -16, 16, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(5))),
              getOnHover(TextComponentUtil.build(Translation.of("gui.mekanism.stockControl"), ": ", OnOff.of(tileEntity.stockControl)))));
        addButton(fillEmptyButton = new DisableableImageButton(guiLeft + 44, guiTop + 75, 16, 16, 238, 16, -16, 16, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(4))),
              getOnHover("gui.mekanism.fillEmpty")));
        addButton(craftSingleButton = new DisableableImageButton(guiLeft + 71, guiTop + 75, 16, 16, 190, 16, -16, 16, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(2))),
              getOnHover("gui.mekanism.craftSingle")));
        addButton(craftAvailableButton = new DisableableImageButton(guiLeft + 89, guiTop + 75, 16, 16, 206, 16, -16, 16, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(3))),
              getOnHover("gui.mekanism.craftAvailable")));
        addButton(autoModeButton = new DisableableImageButton(guiLeft + 107, guiTop + 75, 16, 16, 222, 16, -16, 16, getGuiLocation(),
              onPress -> Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, TileNetworkList.withContents(0))),
              getOnHover(TextComponentUtil.build(Translation.of("gui.mekanism.autoModeToggle"), ": ", OnOff.of(tileEntity.autoMode)))));
        updateEnabledButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateEnabledButtons();
    }

    private void updateEnabledButtons() {
        encodeFormulaButton.active = !tileEntity.autoMode && tileEntity.isRecipe && canEncode();
        stockControlButton.active = tileEntity.formula != null;
        fillEmptyButton.active = !tileEntity.autoMode;
        craftSingleButton.active = !tileEntity.autoMode && tileEntity.isRecipe;
        craftAvailableButton.active = !tileEntity.autoMode && tileEntity.isRecipe;
        autoModeButton.active = tileEntity.formula != null;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawString(tileEntity.getName(), (xSize / 2) - (getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        drawString(TextComponentUtil.translate("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        if (tileEntity.operatingTicks > 0) {
            int display = (int) ((double) tileEntity.operatingTicks * 22 / (double) tileEntity.ticksRequired);
            drawTexturedRect(guiLeft + 86, guiTop + 43, 176, 48, display, 16);
        }

        minecraft.textureManager.bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "slot.png"));
        drawTexturedRect(guiLeft + 90, guiTop + 25, tileEntity.isRecipe ? 2 : 20, 39, 14, 12);

        if (tileEntity.formula != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = tileEntity.formula.input.get(i);
                if (!stack.isEmpty()) {
                    Slot slot = container.inventorySlots.get(i + 20);
                    int guiX = guiLeft + slot.xPos;
                    int guiY = guiTop + slot.yPos;
                    if (slot.getStack().isEmpty() || !tileEntity.formula.isIngredientInPos(tileEntity.getWorld(), slot.getStack(), i)) {
                        drawColorIcon(guiX, guiY, EnumColor.DARK_RED, 0.8F);
                    }
                    renderItem(stack, guiX, guiY);
                }
            }
        }
    }

    private boolean canEncode() {
        if (tileEntity.formula != null) {
            return false;
        }
        ItemStack formulaStack = tileEntity.getInventory().get(TileEntityFormulaicAssemblicator.SLOT_FORMULA);
        return !formulaStack.isEmpty() && formulaStack.getItem() instanceof ItemCraftingFormula && ((ItemCraftingFormula) formulaStack.getItem()).getInventory(formulaStack) == null;
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "formulaic_assemblicator.png");
    }
}