package com.robcholz.lumen.client.config;

import com.robcholz.lumen.client.SerialPortLocator;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class LumenConfigScreen extends Screen {
    private static final int ENTRY_HEIGHT = 20;
    private static final int LIST_PADDING = 2;
    private final Screen parent;
    private final LumenConfig workingConfig;
    private TextFieldWidget portField;
    private TextFieldWidget reconnectField;
    private boolean autoReconnectEnabled;
    private ButtonWidget autoReconnectButton;
    private ButtonWidget portSelectButton;
    private ButtonWidget refreshPortsButton;
    private List<String> portOptions = List.of();
    private boolean showPortList;
    private String lastPortFieldValue = "";
    private int portListX;
    private int portListY;
    private int portListWidth;
    private int scrollOffset;

    public LumenConfigScreen(Screen parent) {
        super(Text.translatable("lumen.title"));
        this.parent = parent;
        this.workingConfig = LumenConfigManager.get().copy();
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = height / 4;

        portField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.translatable("lumen.config.port_path"));
        portField.setMaxLength(256);
        portField.setText(workingConfig.portPath);
        addSelectableChild(portField);

        y += 26;
        portSelectButton = ButtonWidget.builder(portSelectLabel(), button -> {
            showPortList = !showPortList;
            if (showPortList) {
                refreshPortOptions();
                scrollOffset = 0;
            }
        }).dimensions(centerX - 100, y, 200, 20).build();
        addDrawableChild(portSelectButton);
        portListX = centerX - 100;
        portListY = y + 22;
        portListWidth = 200;

        y += 24;
        refreshPortsButton = ButtonWidget.builder(Text.translatable("lumen.config.refresh_ports"), button -> {
            refreshPortOptions();
            scrollOffset = 0;
        }).dimensions(centerX - 100, y, 200, 20).build();
        addDrawableChild(refreshPortsButton);

        y += 30;
        autoReconnectEnabled = workingConfig.autoReconnect;
        autoReconnectButton = ButtonWidget.builder(autoReconnectLabel(), button -> {
            autoReconnectEnabled = !autoReconnectEnabled;
            autoReconnectButton.setMessage(autoReconnectLabel());
        }).dimensions(centerX - 100, y, 200, 20).build();
        addDrawableChild(autoReconnectButton);

        y += 26;
        reconnectField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.translatable("lumen.config.reconnect_period"));
        reconnectField.setMaxLength(4);
        reconnectField.setText(Integer.toString(workingConfig.reconnectPeriodSeconds));
        addSelectableChild(reconnectField);

        int buttonY = height - 50;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> saveAndClose())
                .dimensions(centerX - 100, buttonY, 95, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(centerX + 5, buttonY, 95, 20)
                .build());

        refreshPortOptions();
        setInitialFocus(portField);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void saveAndClose() {
        String port = portField.getText().trim();
        workingConfig.portPath = port;
        workingConfig.autoReconnect = autoReconnectEnabled;
        workingConfig.reconnectPeriodSeconds = parseReconnectPeriod(reconnectField.getText());
        LumenConfigManager.save(workingConfig);
        close();
    }

    private int parseReconnectPeriod(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(1, value);
        } catch (NumberFormatException e) {
            return Math.max(1, workingConfig.reconnectPeriodSeconds);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("lumen.config.serial_settings"), width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("lumen.config.leave_port_blank"), width / 2 - 100, height / 4 - 12, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
        if (showPortList) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 400);
            renderPortList(context, mouseX, mouseY);
            context.getMatrices().pop();
        }
    }

    @Override
    public void tick() {
        super.tick();
        String current = portField.getText();
        if (!current.equals(lastPortFieldValue)) {
            lastPortFieldValue = current;
            portSelectButton.setMessage(portSelectLabel());
        }
    }

    private Text autoReconnectLabel() {
        Text state = autoReconnectEnabled
                ? Text.translatable("lumen.config.on")
                : Text.translatable("lumen.config.off");
        return Text.translatable("lumen.config.auto_reconnect", state);
    }

    private Text portSelectLabel() {
        String value = portField.getText().trim();
        Text display = value.isEmpty()
                ? Text.translatable("lumen.config.auto_detect")
                : Text.literal(value);
        return Text.translatable("lumen.config.select_port", display);
    }

    private void refreshPortOptions() {
        portOptions = SerialPortLocator.listPorts();
    }

    private void renderPortList(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY) {
        int listSize = totalPortEntries();
        int visibleEntries = Math.min(listSize, maxVisibleEntries());
        int listHeight = visibleEntries * ENTRY_HEIGHT + LIST_PADDING * 2;
        int x1 = portListX;
        int y1 = portListY;
        int x2 = portListX + portListWidth;
        int y2 = portListY + listHeight;

        context.fill(x1, y1, x2, y2, 0xFF3A3A3A);
        context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF111111);

        int startIndex = clampScrollOffset(visibleEntries, listSize);
        int endIndex = Math.min(listSize, startIndex + visibleEntries);
        for (int index = startIndex; index < endIndex; index++) {
            Text label = portLabelForIndex(index);
            drawPortEntry(context, mouseX, mouseY, index - startIndex, label);
        }

        if (listSize > visibleEntries) {
            drawScrollBar(context, x2 - 5, y1 + 1, listHeight - 2, startIndex, listSize, visibleEntries);
        }
    }

    private void drawPortEntry(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, int index, Text label) {
        int x = portListX + LIST_PADDING;
        int y = portListY + LIST_PADDING + index * ENTRY_HEIGHT;
        int width = portListWidth - LIST_PADDING * 2;
        int height = ENTRY_HEIGHT;
        boolean hover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (hover) {
            context.fill(x, y, x + width, y + height, 0xFF3A3A3A);
        }
        context.drawTextWithShadow(textRenderer, label, x + 4, y + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showPortList) {
            if (handlePortListClick(mouseX, mouseY)) {
                return true;
            }
            showPortList = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePortListClick(double mouseX, double mouseY) {
        int listSize = totalPortEntries();
        int visibleEntries = Math.min(listSize, maxVisibleEntries());
        int listHeight = visibleEntries * ENTRY_HEIGHT + LIST_PADDING * 2;
        int x1 = portListX + LIST_PADDING;
        int y1 = portListY + LIST_PADDING;
        int x2 = portListX + portListWidth - LIST_PADDING;
        int y2 = portListY + listHeight - LIST_PADDING;
        if (mouseX < x1 || mouseX > x2 || mouseY < y1 || mouseY > y2) {
            return false;
        }
        int startIndex = clampScrollOffset(visibleEntries, listSize);
        int index = (int) ((mouseY - y1) / ENTRY_HEIGHT) + startIndex;
        if (index == 0) {
            portField.setText("");
            portSelectButton.setMessage(portSelectLabel());
            showPortList = false;
            return true;
        }
        if (portOptions.isEmpty()) {
            return true;
        }
        int portIndex = index - 1;
        if (portIndex >= 0 && portIndex < portOptions.size()) {
            portField.setText(portOptions.get(portIndex));
            portSelectButton.setMessage(portSelectLabel());
            showPortList = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showPortList) {
            int listSize = totalPortEntries();
            int visibleEntries = Math.min(listSize, maxVisibleEntries());
            if (listSize > visibleEntries) {
                int delta = verticalAmount > 0 ? -1 : 1;
                scrollOffset = Math.max(0, Math.min(scrollOffset + delta, listSize - visibleEntries));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int totalPortEntries() {
        return 1 + Math.max(1, portOptions.size());
    }

    private int maxVisibleEntries() {
        int maxPixels = Math.max(ENTRY_HEIGHT * 2, height - portListY - 60);
        return Math.max(1, maxPixels / ENTRY_HEIGHT);
    }

    private int clampScrollOffset(int visibleEntries, int listSize) {
        int maxOffset = Math.max(0, listSize - visibleEntries);
        if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
        return scrollOffset;
    }

    private Text portLabelForIndex(int index) {
        if (index == 0) {
            return Text.translatable("lumen.config.auto_detect");
        }
        if (portOptions.isEmpty()) {
            return Text.translatable("lumen.config.no_ports_found");
        }
        int portIndex = index - 1;
        if (portIndex >= 0 && portIndex < portOptions.size()) {
            return Text.literal(portOptions.get(portIndex));
        }
        return Text.empty();
    }

    private void drawScrollBar(net.minecraft.client.gui.DrawContext context, int x, int y, int height, int startIndex, int listSize, int visibleEntries) {
        int trackHeight = height;
        int thumbHeight = Math.max(12, (trackHeight * visibleEntries) / listSize);
        int maxOffset = Math.max(1, listSize - visibleEntries);
        int thumbY = y + (trackHeight - thumbHeight) * startIndex / maxOffset;
        context.fill(x, y, x + 3, y + height, 0xFF202020);
        context.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xFF8A8A8A);
    }
}
