package cofh.thermalexpansion.gui.client.ender;

import cofh.api.tileentity.ISecurable.AccessMode;
import cofh.core.RegistryEnderAttuned;
import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabInfo;
import cofh.core.gui.element.TabRedstone;
import cofh.core.gui.element.TabSecurity;
import cofh.core.gui.element.TabTutorial;
import cofh.lib.gui.element.ElementButton;
import cofh.lib.gui.element.ElementListBox;
import cofh.lib.gui.element.ElementSlider;
import cofh.lib.gui.element.ElementTextField;
import cofh.lib.gui.element.ElementTextFieldLimited;
import cofh.lib.gui.element.listbox.IListBoxElement;
import cofh.lib.gui.element.listbox.SliderVertical;
import cofh.lib.transport.IEnderChannelRegistry;
import cofh.lib.transport.IEnderChannelRegistry.Frequency;
import cofh.lib.util.helpers.SecurityHelper;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.block.ender.TileTesseract;
import cofh.thermalexpansion.core.TEProps;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.gui.element.ListBoxElementEnderText;
import cofh.thermalexpansion.gui.element.TabConfigTesseract;

import java.util.UUID;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

public class GuiTesseract extends GuiBaseAdv {

	static final String TEX_PATH = TEProps.PATH_GUI_ENDER + "Tesseract.png";

	static final int TB_HEIGHT = 12;

	TileTesseract myTile;
	AccessMode tileAccess;
	boolean requested;
	UUID playerName;

	int updated;

	ElementListBox frequencies;
	ElementSlider slider;
	ElementTextField freq;
	ElementTextField name;

	ElementButton assign;
	ElementButton clear;
	ElementButton add;
	ElementButton remove;

	public GuiTesseract(InventoryPlayer inventory, TileEntity theTile) {

		super(new ContainerTEBase(inventory, theTile, false, false), new ResourceLocation(TEX_PATH));
		myTile = (TileTesseract) theTile;
		super.name = myTile.getInventoryName();
		playerName = SecurityHelper.getID(inventory.player);
		drawInventory = false;
	}

	@Override
	public void initGui() {

		super.initGui();
		Keyboard.enableRepeatEvents(true);
		tileAccess = myTile.getAccess();
		RegistryEnderAttuned.requestChannelList(myTile.getChannelString());

		addTab(new TabRedstone(this, myTile));
		addTab(new TabConfigTesseract(this, myTile));

		generateInfo("tab.thermalexpansion.ender.tesseract", 2);
		addTab(new TabInfo(this, myInfo));
		addTab(new TabTutorial(this, StringHelper.tutorialTabRedstone() + "\n\n" + StringHelper.tutorialTabConfigurationOperation()));
		if (myTile.enableSecurity() && myTile.isSecured()) {
			addTab(new TabSecurity(this, myTile, playerName));
		}

		int tempFreq = myTile.getFrequency();
		addElement(freq = new ElementTextFieldLimited(this, 102, 27, 26, 11, (short) 3).setFilter("0123456789", false).setBackgroundColor(0, 0, 0)
				.setText(tempFreq >= 0 ? String.valueOf(tempFreq) : ""));
		addElement(name = new ElementTextField(this, 8, 42, 128, 11, (short) 30).setBackgroundColor(0, 0, 0));

		addElement(assign = new ElementButton(this, 131, 18, 20, 20, 208, 192, 208, 212, 176, 40, TEX_PATH) {

			@Override
			public void onClick() {

				int tempFreq = Integer.parseInt(freq.getText());
				myTile.setTileInfo(tempFreq);
			}
		}.setToolTip("info.cofh.setFrequency"));
		addElement(clear = new ElementButton(this, 151, 18, 20, 20, 228, 192, 228, 212, 196, 40, TEX_PATH) {

			@Override
			public void onClick() {

				myTile.setTileInfo(-1);
			}
		}.setToolTip("info.cofh.disable"));

		addElement(add = new ElementButton(this, 139, 40, 16, 16, 208, 128, 208, 144, 176, 92, TEX_PATH) {

			@Override
			public void onClick() {

				int tempFreq = Integer.parseInt(freq.getText());
				RegistryEnderAttuned.getChannels(false).setFrequency(myTile.getChannelString(), tempFreq, GuiTesseract.this.name.getText());
				myTile.addEntry(tempFreq, GuiTesseract.this.name.getText());
			}
		}.setToolTip("info.cofh.addFrequency"));
		addElement(remove = new ElementButton(this, 155, 40, 16, 16, 224, 128, 224, 144, 192, 92, TEX_PATH) {

			@Override
			public void onClick() {

				int tempFreq = Integer.parseInt(freq.getText());
				RegistryEnderAttuned.getChannels(false).removeFrequency(myTile.getChannelString(), tempFreq);
				myTile.removeEntry(tempFreq, GuiTesseract.this.name.getText());
			}
		}.setToolTip("info.cofh.removeFrequency"));

		addElement(frequencies = new ElementListBox(this, 7, 57, 130, 104) {

			@Override
			protected void onElementClicked(IListBoxElement element) {

				Frequency freq = (Frequency) element.getValue();
				GuiTesseract.this.name.setText(freq.name);
				GuiTesseract.this.freq.setText(String.valueOf(freq.freq));
			}

			@Override
			protected void onScrollV(int newStartIndex) {

				slider.setValue(newStartIndex);
			}

			@Override
			protected int drawElement(int elementIndex, int x, int y) {

				IListBoxElement element = _elements.get(elementIndex);
				if (((Frequency) element.getValue()).freq == myTile.getFrequency()) {
					element.draw(this, x, y, 1, selectedTextColor);
				} else if (elementIndex == _selectedIndex) {
					element.draw(this, x, y, selectedLineColor, selectedTextColor);
				} else {
					element.draw(this, x, y, backgroundColor, textColor);
				}

				return element.getHeight();
			}

		}.setBackgroundColor(0, 0));
		frequencies.setSelectedIndex(-1);
		IEnderChannelRegistry data = RegistryEnderAttuned.getChannels(false);
		updated = data.updated();
		for (Frequency freq : data.getFrequencyList(null)) {
			frequencies.add(new ListBoxElementEnderText(freq));
			if (freq.freq == myTile.getFrequency()) {
				frequencies.setSelectedIndex(frequencies.getElementCount() - 1);
				this.name.setText(freq.name);
			}
		}
		addElement(slider = new SliderVertical(this, 137, 58, 14, 102, frequencies.getLastScrollPosition()) {

			@Override
			public void onValueChanged(int value) {

				frequencies.scrollToV(value);
			}

		}.setColor(0, 0));
	}

	@Override
	public void onGuiClosed() {

		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}

	@Override
	public void updateScreen() {

		super.updateScreen();

		if (!myTile.canAccess()) {
			this.mc.thePlayer.closeScreen();
		}
	}

	@Override
	protected void updateElementInformation() {

		IEnderChannelRegistry data = RegistryEnderAttuned.getChannels(false);
		if (updated != data.updated()) {
			updated = data.updated();
			requested = false;
			IListBoxElement ele = frequencies.getSelectedElement();
			int sel = ele != null ? ((Frequency) ele.getValue()).freq : -1;
			int pos = slider.getSliderY();
			frequencies.removeAll();
			frequencies.setSelectedIndex(-1);
			for (Frequency freq : data.getFrequencyList(null)) {
				frequencies.add(new ListBoxElementEnderText(freq));
				if (freq.freq == sel && String.valueOf(sel).equals(this.freq.getText())) {
					frequencies.setSelectedIndex(frequencies.getElementCount() - 1);
					this.freq.setText(String.valueOf(freq.freq));
					this.name.setText(freq.name);
				}
			}
			slider.setLimits(0, frequencies.getLastScrollPosition());
			slider.setValue(pos);
		} else if (!requested && tileAccess != myTile.getAccess()) {
			requested = true;
			tileAccess = myTile.getAccess();
			RegistryEnderAttuned.requestChannelList(myTile.getChannelString());
		}

		boolean hasFreq = freq.getContentLength() > 0, hasName = name.getContentLength() > 0;
		assign.setEnabled(hasFreq && !String.valueOf(myTile.getFrequency()).equals(freq.getText()));
		clear.setEnabled(myTile.getFrequency() != -1);
		add.setEnabled(hasName && hasFreq
				&& !name.getText().equals(RegistryEnderAttuned.getChannels(false).getFrequency(null, Integer.parseInt(freq.getText()))));
		remove.setEnabled(hasFreq && hasName
				&& name.getText().equals(RegistryEnderAttuned.getChannels(false).getFrequency(null, Integer.parseInt(freq.getText()))));
	}

}
