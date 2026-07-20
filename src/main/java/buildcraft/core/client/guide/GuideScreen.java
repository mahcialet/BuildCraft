package buildcraft.core.client.guide;

import buildcraft.core.client.guide.GuideRepository.Page;
import buildcraft.core.item.ItemGuideNote;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

/** Searchable two-page reader for the original BuildCraftGuide resource pack. */
public final class GuideScreen extends Screen {
    private static final int BOOK_WIDTH = 386;
    private static final int BOOK_HEIGHT = 248;
    private final ItemStack source;
    private final boolean note;
    private final Deque<Page> back = new ArrayDeque<>();
    private final Deque<Page> forward = new ArrayDeque<>();
    private List<Page> allPages = List.of();
    private List<Page> matches = List.of();
    private List<FormattedCharSequence> lines = List.of();
    private Page page;
    private EditBox search;
    private int sheet;

    public GuideScreen(ItemStack source, boolean note) {
        super(Component.translatable("gui.buildcraftcore.guide.title"));
        this.source = source.copy();
        this.note = note;
    }

    @Override
    protected void init() {
        allPages = GuideRepository.load();
        int left = (width - BOOK_WIDTH) / 2;
        int top = (height - BOOK_HEIGHT) / 2;
        search = new EditBox(font, left + 25, top + 20, 155, 16,
            Component.translatable("gui.buildcraftcore.guide.search"));
        search.setHint(Component.translatable("gui.buildcraftcore.guide.search"));
        search.setResponder(this::search);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> historyBack())
            .bounds(left + 164, top + 222, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> historyForward())
            .bounds(left + 198, top + 222, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("⌂"), b -> page = null)
            .bounds(left + 232, top + 222, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("‹"), b -> setSheet(sheet - 1))
            .bounds(left + 310, top + 222, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal("›"), b -> setSheet(sheet + 1))
            .bounds(left + 338, top + 222, 24, 16).build());

        String noteId = note ? ItemGuideNote.note(source) : "";
        if (!noteId.isEmpty()) {
            allPages.stream().filter(candidate -> candidate.id().equals(noteId)).findFirst().ifPresent(this::openInitial);
        }
        search("");
    }

    private void search(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        matches = allPages.stream().filter(candidate -> needle.isEmpty() || candidate.searchable().contains(needle))
            .limit(14).toList();
    }

    private void openInitial(Page target) {
        page = target;
        rebuildLines();
    }

    private void open(Page target) {
        if (page != null) back.push(page);
        forward.clear();
        openInitial(target);
    }

    private void historyBack() {
        if (back.isEmpty()) return;
        if (page != null) forward.push(page);
        openInitial(back.pop());
    }

    private void historyForward() {
        if (forward.isEmpty()) return;
        if (page != null) back.push(page);
        openInitial(forward.pop());
    }

    private void rebuildLines() {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        if (page != null) for (String paragraph : page.paragraphs()) {
            wrapped.addAll(font.split(Component.literal(paragraph), 158));
            wrapped.add(FormattedCharSequence.EMPTY);
        }
        lines = List.copyOf(wrapped);
        sheet = 0;
    }

    private void setSheet(int value) {
        int max = Math.max(0, (lines.size() - 1) / 36);
        sheet = Math.max(0, Math.min(max, value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - BOOK_WIDTH) / 2;
        int top = (height - BOOK_HEIGHT) / 2;
        graphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, 0xffc8ae7b);
        graphics.fill(left + 193, top + 4, left + 195, top + 220, 0xff6e4d2e);
        graphics.text(font, title, left + 25, top + 6, 0x3b2818, false);

        if (page == null) {
            int y = top + 43;
            for (int index = 0; index < matches.size(); index++) {
                Page candidate = matches.get(index);
                int rowY = y + index * 12;
                int color = mouseX >= left + 24 && mouseX < left + 184 && mouseY >= rowY && mouseY < rowY + 11
                    ? 0xa04620 : 0x3b2818;
                graphics.text(font, Component.literal(candidate.title()), left + 25, rowY, color, false);
            }
        }

        if (page == null) {
            graphics.text(font, Component.translatable("gui.buildcraftcore.guide.select"), left + 213, top + 25,
                0x3b2818, false);
        } else {
            graphics.text(font, Component.literal(page.title()), left + 25, top + 15, 0x3b2818, false);
            graphics.text(font, Component.literal(page.category()), left + 213, top + 28, 0x765734, false);
            int start = sheet * 36;
            for (int index = start; index < Math.min(lines.size(), start + 36); index++) {
                int column = (index - start) / 18;
                int row = (index - start) % 18;
                graphics.text(font, lines.get(index), left + 25 + column * 188, top + 45 + row * 10,
                    0x281c12, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int left = (width - BOOK_WIDTH) / 2;
        int top = (height - BOOK_HEIGHT) / 2;
        int index = (int) ((mouseY - (top + 43)) / 12);
        if (page == null && event.button() == 0 && mouseX >= left + 24 && mouseX < left + 184
            && index >= 0 && index < matches.size()) {
            open(matches.get(index));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
