package org.agmas.pathsrole.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.agmas.pathsrole.Paths;
import org.agmas.pathsrole.client.screen.PathStoryDetailScreen.CharacterStoryData;

@Environment(value = EnvType.CLIENT)
public class PathStoryDetailScreen extends Screen {

    private static final int VOID = 0xFF080B14;
    private static final int INK = 0xFF0F1428;
    private static final int PANEL = 0xFF0F1828;
    private static final int PANEL_SOFT = 0xFF182440;
    private static final int BRONZE = 0xFF8B6914;
    private static final int GOLD_DARK = 0xFF6B5A30;
    private static final int GOLD = 0xFFD4AF37;
    private static final int GOLD_BRIGHT = 0xFFFFE1A0;
    private static final int IVORY = 0xFFF7EBCF;
    private static final int TEXT = 0xFFFFF4DC;
    private static final int TEXT_MUTED = 0xFF9E8B6E;

    private static final float LEFT_PANEL_RATIO = 0.32f;
    private static final int LIST_ITEM_HEIGHT = 42;
    private static final int LIST_ITEM_PADDING = 4;
    private static final int LIST_HEADER_HEIGHT = 36;

    private final Paths selectedPath;
    private final List<CharacterStoryData> characters;
    private int selectedCharIndex = 0;
    private double scrollOffset = 0.0;
    private double scrollTarget = 0.0;
    private boolean isScrolling = false;
    private double scrollStartY = 0.0;
    
    private double storyScrollOffset = 0.0;
    private double storyScrollTarget = 0.0;
    
    private long openedAtMillis;
    private long lastFrameMillis;

    private int backButtonX, backButtonY, backButtonW, backButtonH;

    private static final Map<Paths, List<CharacterStoryData>> PATH_CHARACTERS = Map.of(
        Paths.EQUILIBRIUM, List.of(
            new CharacterStoryData(
                "灵梦·博丽",
                "万物皆有其平衡，而我，即是这平衡的守护者。",
                "《幻想乡缘起》",
                List.of(
                    "在遥远的东方，存在着一处被结界隔绝的幻想之乡。",
                    "那里的居民们遵循着独特的法则，而维持这份微妙平衡的，",
                    "正是红白巫女——博丽灵梦。",
                    "",
                    "某日，时空的裂隙悄然开启。",
                    "灵梦在解决异变的过程中被卷入了未知的维度，",
                    "当她再次睁开眼时，发现自己来到了这个充满命途之力的新世界。",
                    "",
                    "这里的「均衡」之道与她所守护的幻想乡有着奇妙的共鸣。",
                    "作为能够操控灵力与阴阳的巫女，她天生便对各种极端力量保持着中立的立场。",
                    "无论是毁灭的狂暴、巡猎的执着，还是虚无的消解，",
                    "在她眼中都不过是需要被制衡的存在。",
                    "",
                    "在这个陌生的世界里，灵梦并未迷失方向。",
                    "她以符咒与御币为武器，继续履行着守护者的职责——",
                    "不是偏向任何一方，而是确保天平永不倾覆。",
                    "",
                    "或许有一天，当所有命途的力量达到完美的均衡之时，",
                    "那条通往故乡的道路，便会重新显现……"
                )
            )
        ),
        Paths.THE_HUNT, List.of(
            new CharacterStoryData(
                "赏金猎人",
                "银河之大，最不缺的就是亡命的恶徒，他们是能动的钱袋子。",
                "《慧星酒吧悬赏榜》",
                List.of(
                    "在慧星酒吧的一角，有一张常年被阴影笼罩的桌子。",
                    "桌上放着一把擦拭得锃亮的左轮手枪，和一叠空白的通缉令。",
                    "桌子的主人，自称「赏金猎人」。",
                    "",
                    "没人知道他的真名，只知道他是酒吧开业以来，",
                    "完成率最高的猎手——只要上了他的名单，就没有能活着见到第二天的。",
                    "有人说他为了钱什么事都做得出来，",
                    "但熟悉他的人都知道，他只接「该死的买卖」。",
                    "",
                    "他与「巡猎」命途的共鸣，并非源于对正义的狂热追求，",
                    "而是源自一种古老而朴素的信念：",
                    "恶人必须付出代价，而赏金，不过是顺手为之。",
                    "",
                    "他的猎杀之道冷酷而高效。",
                    "一张通缉令，一个名字，一把枪，一颗子弹。",
                    "从不失手，也从不越过自己的底线。",
                    "在杀手的行列中，他是一道独特而孤独的风景——",
                    "既不属于纯粹的正义，也不堕入彻底的疯狂。",
                    "",
                    "或许有一天，当银河中再无值得他出手的猎物，",
                    "他会放下那把左轮，在某个星球的落日余晖中，",
                    "喝一杯不加冰的威士忌，然后继续寻找下一个猎物。",
                    "因为他知道——在这片星海中，恶人永远不会绝迹。"
                )
            )
        ),
        Paths.ELATION, List.of(
            new CharacterStoryData(
                "磕学家",
                "世间最极致的欢愉，莫过于见证两颗心的相遇与交织。",
                "《恋爱心理学导论》",
                List.of(
                    "在众多命途的追随者中，存在着这样一群特殊的人——",
                    "他们自称为「磕学家」。",
                    "",
                    "他们不追求毁灭的力量，也不渴望智慧的真理，",
                    "更无意于存护或开拓的伟业。",
                    "他们的唯一信条，便是追寻并见证世间最美好的情感羁绊——",
                    "也就是俗称的「CP」。",
                    "",
                    "这位磕学家原本只是个普通的观测者，",
                    "直到她发现了「欢愉」命途的奥义：",
                    "真正的快乐并非来自物质的丰盈，",
                    "而是源于灵魂共鸣时那瞬间的璀璨火花。",
                    "",
                    "于是她开始游历诸界，用独特的「CP雷达」感知那些命中注定的相遇。",
                    "无论是英雄与反宿命般的对立吸引，",
                    "还是同伴间默默守护的深情厚谊，",
                    "亦或是跨越阵营的禁忌之恋，",
                    "都在她的观察记录册上留下了浓墨重彩的一笔。",
                    "",
                    "有人说她是个多管闲事的旁观者，",
                    "也有人视她为爱情魔法的实践者。",
                    "但她毫不在意这些评价，",
                    "因为她知道——每一对被她见证的眷侣，",
                    "都会在这浩瀚星海中，点亮属于他们的欢愉之光。"
                )
            )
        )
    );

    public record CharacterStoryData(String name, String quote, String quoteSource, List<String> story) {}

    public PathStoryDetailScreen(Paths path) {
        super(Component.literal(path.getChineseName() + " - " 
            + Component.translatable("screen.pathsrole.path_story_collection").getString()));
        this.selectedPath = path;
        this.characters = PATH_CHARACTERS.getOrDefault(path, List.of());
        this.openedAtMillis = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        this.backButtonW = 60;
        this.backButtonH = 20;
        this.backButtonX = this.width - this.backButtonW - 15;
        this.backButtonY = 8;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.pathsrole.back"), b -> onBack())
            .bounds(this.backButtonX, this.backButtonY, this.backButtonW, this.backButtonH)
            .build());

        this.lastFrameMillis = System.currentTimeMillis();

        if (this.selectedCharIndex >= this.characters.size()) {
            this.selectedCharIndex = 0;
        }
        if (this.characters.isEmpty()) {
            this.selectedCharIndex = -1;
        }
        
        this.storyScrollOffset = 0.0;
        this.storyScrollTarget = 0.0;
    }

    private void onBack() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PathStoryCollectionScreen());
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PathStoryCollectionScreen());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float frameSeconds = (now - this.lastFrameMillis) / 1000.0f;
        float elapsedSeconds = (now - this.openedAtMillis) / 1000.0f;
        this.lastFrameMillis = now;

        updateScroll(frameSeconds);

        renderBackdrop(g, elapsedSeconds);
        renderHeader(g, elapsedSeconds);
        renderLeftPanel(g, mouseX, mouseY, elapsedSeconds);
        renderRightPanel(g, mouseX, mouseY, elapsedSeconds);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    private void updateScroll(float frameSeconds) {
        if (!this.isScrolling) {
            double diff = this.scrollTarget - this.scrollOffset;
            if (Math.abs(diff) > 0.1) {
                this.scrollOffset += diff * approachFactor(frameSeconds, 12.0f);
            } else {
                this.scrollOffset = this.scrollTarget;
            }
        }

        int listHeight = this.characters.size() * (LIST_ITEM_HEIGHT + LIST_ITEM_PADDING) + LIST_ITEM_PADDING;
        int panelContentHeight = this.height - LIST_HEADER_HEIGHT - 60;
        double maxScroll = Math.max(0, listHeight - panelContentHeight);

        if (this.scrollOffset < 0) {
            this.scrollOffset = 0;
            this.scrollTarget = 0;
        } else if (this.scrollOffset > maxScroll) {
            this.scrollOffset = maxScroll;
            this.scrollTarget = maxScroll;
        }
    }

    private void renderBackdrop(GuiGraphics g, float elapsedSeconds) {
        g.fill(0, 0, this.width, this.height, 0xFF080B14);
        g.fillGradient(0, 0, this.width, this.height, 0xFF080B14, 0xFF0F1428);
    }

    private void renderHeader(GuiGraphics g, float elapsedSeconds) {
        String title = this.selectedPath.getChineseName() + " - " 
            + Component.translatable("screen.pathsrole.path_story_collection").getString();
        drawStringWithShadow(g, title, this.width / 2, 12, IVORY, true);

        float reveal = easeOutCubic(Mth.clamp(elapsedSeconds / 0.65f, 0.0f, 1.0f));
        int revealHalf = Math.round(this.width * 0.5f * reveal);
        g.hLine(this.width / 2 - revealHalf, this.width / 2 + revealHalf, 32, withAlpha(GOLD, 100));
    }

    private void renderLeftPanel(GuiGraphics g, int mouseX, int mouseY, float elapsedSeconds) {
        int panelWidth = Math.round(this.width * LEFT_PANEL_RATIO);
        int panelX = 12;
        int panelY = 42;
        int panelHeight = this.height - panelY - 12;

        fillChamfered(g, panelX - 1, panelY + 2, panelWidth + 2, panelHeight + 2, 6, withAlpha(0xFF000000, 80));
        fillChamfered(g, panelX, panelY, panelWidth, panelHeight, 6, withAlpha(BRONZE, 220));
        fillChamfered(g, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 5, withAlpha(PANEL_SOFT, 255));
        g.fillGradient(panelX + 3, panelY + 4, panelX + panelWidth - 3, panelY + panelHeight - 4,
            PANEL, PANEL);

        String headerText = Component.translatable("gui.pathsrole.characters").getString();
        drawStringWithShadow(g, headerText, panelX + 10, panelY + 10, TEXT, false);
        g.hLine(panelX + 8, panelX + panelWidth - 8, panelY + LIST_HEADER_HEIGHT, withAlpha(GOLD, 80));

        if (this.characters.isEmpty()) {
            String emptyText = Component.translatable("gui.pathsrole.no_characters").getString();
            int textWidth = this.font.width(emptyText);
            drawStringWithShadow(g, emptyText, panelX + (panelWidth - textWidth) / 2, 
                panelY + panelHeight / 2 - 4, TEXT, false);
            return;
        }

        int listStartY = panelY + LIST_HEADER_HEIGHT + 8;
        int listAreaHeight = panelHeight - LIST_HEADER_HEIGHT - 16;

        g.enableScissor(panelX + 3, listStartY, panelX + panelWidth - 3, listStartY + listAreaHeight);

        for (int i = 0; i < this.characters.size(); i++) {
            CharacterStoryData ch = this.characters.get(i);
            int itemY = listStartY + i * (LIST_ITEM_HEIGHT + LIST_ITEM_PADDING) + LIST_ITEM_PADDING - (int) this.scrollOffset;
            int itemX = panelX + 8;
            int itemWidth = panelWidth - 16;

            boolean isSelected = (i == this.selectedCharIndex);
            boolean isHovered = mouseX >= itemX && mouseX <= itemX + itemWidth
                && mouseY >= itemY && mouseY <= itemY + LIST_ITEM_HEIGHT;

            int itemBgColor = isSelected ? 0xFFFFFFFF : (isHovered ? GOLD_DARK : PANEL);
            int itemAlpha = isSelected ? 255 : (isHovered ? 180 : 120);

            fillChamfered(g, itemX, itemY, itemWidth, LIST_ITEM_HEIGHT, 4, withAlpha(itemBgColor, itemAlpha));

            if (isSelected) {
                drawDiamond(g, itemX + 6, itemY + LIST_ITEM_HEIGHT / 2, 3, withAlpha(0xFF000000, 255));
            }

            int textColor = isSelected ? 0xFF000000 : (isHovered ? GOLD_BRIGHT : TEXT);
            int textY = itemY + LIST_ITEM_HEIGHT / 2 - 4;
            drawStringWithShadow(g, ch.name(), itemX + (isSelected ? 16 : 10), textY, textColor, false);

            String pathLabel = this.selectedPath.getChineseName();
            int pathLabelWidth = this.font.width(pathLabel);
            int labelX = itemX + itemWidth - pathLabelWidth - 8;
            int labelY = itemY + LIST_ITEM_HEIGHT - 12;
            drawStringWithShadow(g, pathLabel, labelX, labelY, withAlpha(textColor, 200), false);
        }

        g.disableScissor();
    }

    private void renderRightPanel(GuiGraphics g, int mouseX, int mouseY, float elapsedSeconds) {
        int panelX = Math.round(this.width * LEFT_PANEL_RATIO) + 22;
        int panelWidth = this.width - panelX - 12;
        int panelY = 42;
        int panelHeight = this.height - panelY - 12;

        fillChamfered(g, panelX - 1, panelY + 2, panelWidth + 2, panelHeight + 2, 6, withAlpha(0xFF000000, 80));
        fillChamfered(g, panelX, panelY, panelWidth, panelHeight, 6, withAlpha(BRONZE, 220));
        fillChamfered(g, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 5, withAlpha(PANEL_SOFT, 255));
        g.fillGradient(panelX + 3, panelY + 4, panelX + panelWidth - 3, panelY + panelHeight - 4,
            PANEL, PANEL);

        if (this.characters.isEmpty() || this.selectedCharIndex < 0) {
            String emptyText = Component.translatable("gui.pathsrole.select_character").getString();
            int textWidth = this.font.width(emptyText);
            drawStringWithShadow(g, emptyText, panelX + (panelWidth - textWidth) / 2, 
                panelY + panelHeight / 2 - 4, TEXT, false);
            return;
        }

        CharacterStoryData ch = this.characters.get(this.selectedCharIndex);

        int contentX = panelX + 18;
        int contentY = panelY + 16;
        int contentWidth = panelWidth - 36;

        drawStringWithShadow(g, ch.name(), contentX, contentY, GOLD_BRIGHT, false);
        int nameY = contentY + 14;

        g.hLine(contentX, contentX + contentWidth / 3, nameY + 2, withAlpha(GOLD, 140));

        int quoteY = nameY + 12;
        String quoteText = "「" + ch.quote() + "」";
        drawStringWithShadow(g, quoteText, contentX + 12, quoteY, TEXT, false);

        int sourceY = quoteY + 13;
        String sourceText = "——" + ch.quoteSource();
        drawStringWithShadow(g, sourceText, contentX + 32, sourceY, withAlpha(TEXT, 220), false);

        g.hLine(contentX, contentX + contentWidth / 2, sourceY + 16, withAlpha(GOLD, 100));

        int storyY = sourceY + 32;
        int maxStoryHeight = panelY + panelHeight - storyY - 20;

        g.enableScissor(panelX + 3, storyY, panelX + panelWidth - 3, storyY + maxStoryHeight);

        int lineY = storyY - (int) this.storyScrollOffset;
        for (String paragraph : ch.story()) {
            if (paragraph.isEmpty()) {
                lineY += 8;
                continue;
            }

            List<String> wrappedLines = wrapText(paragraph, contentWidth - 8);
            for (String line : wrappedLines) {
                if (lineY >= storyY - 20 && lineY <= storyY + maxStoryHeight + 10) {
                    drawStringWithShadow(g, line, contentX + 4, lineY, TEXT, false);
                }
                lineY += 11;
            }
            lineY += 3;
        }

        g.disableScissor();
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            String clipped = this.font.plainSubstrByWidth(remaining, maxWidth);
            if (clipped.isEmpty()) {
                break;
            }
            lines.add(clipped);
            remaining = remaining.substring(clipped.length());
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelWidth = Math.round(this.width * LEFT_PANEL_RATIO);
            int panelX = 12;
            int panelY = 42;
            int panelHeight = this.height - panelY - 12;
            int listStartY = panelY + LIST_HEADER_HEIGHT + 8;

            for (int i = 0; i < this.characters.size(); i++) {
                int itemY = listStartY + i * (LIST_ITEM_HEIGHT + LIST_ITEM_PADDING) + LIST_ITEM_PADDING - (int) this.scrollOffset;
                int itemX = panelX + 8;
                int itemWidth = panelWidth - 16;

                if (mouseX >= itemX && mouseX <= itemX + itemWidth
                    && mouseY >= itemY && mouseY <= itemY + LIST_ITEM_HEIGHT) {
                    this.selectedCharIndex = i;
                    this.storyScrollOffset = 0.0;
                    this.storyScrollTarget = 0.0;
                    return true;
                }
            }

            if (mouseX >= panelX && mouseX <= panelX + panelWidth
                && mouseY >= panelY && mouseY <= panelY + panelHeight) {
                this.isScrolling = true;
                this.scrollStartY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isScrolling && button == 0) {
            double dragDelta = mouseY - this.scrollStartY;
            double scrollDelta = -dragDelta;

            int listHeight = this.characters.size() * (LIST_ITEM_HEIGHT + LIST_ITEM_PADDING) + LIST_ITEM_PADDING;
            int panelHeight = this.height - 42 - 12;
            int panelContentHeight = panelHeight - LIST_HEADER_HEIGHT - 16;
            double maxScroll = Math.max(0, listHeight - panelContentHeight);

            this.scrollOffset = Mth.clamp(this.scrollTarget + scrollDelta, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isScrolling) {
            this.isScrolling = false;
            this.scrollTarget = this.scrollOffset;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelWidth = Math.round(this.width * LEFT_PANEL_RATIO);
        int panelX = 12;
        int panelY = 42;
        int panelHeight = this.height - panelY - 12;
        int listStartY = panelY + LIST_HEADER_HEIGHT + 8;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth
            && mouseY >= listStartY && mouseY <= panelY + panelHeight) {

            int listHeight = this.characters.size() * (LIST_ITEM_HEIGHT + LIST_ITEM_PADDING) + LIST_ITEM_PADDING;
            int panelContentHeight = panelHeight - LIST_HEADER_HEIGHT - 16;
            double maxScroll = Math.max(0, listHeight - panelContentHeight);

            this.scrollOffset = Mth.clamp(this.scrollOffset - verticalAmount * 20, 0, maxScroll);
            this.scrollTarget = this.scrollOffset;
            return true;
        }

        int rightPanelX = Math.round(this.width * LEFT_PANEL_RATIO) + 22;
        int rightPanelWidth = this.width - rightPanelX - 12;
        int rightPanelY = 42;
        int rightPanelHeight = this.height - rightPanelY - 12;
        
        if (mouseX >= rightPanelX && mouseX <= rightPanelX + rightPanelWidth
            && mouseY >= rightPanelY && mouseY <= rightPanelY + rightPanelHeight) {
            
            if (this.characters.isEmpty() || this.selectedCharIndex < 0) {
                return false;
            }
            
            CharacterStoryData ch = this.characters.get(this.selectedCharIndex);
            int contentX = rightPanelX + 18;
            int contentY = rightPanelY + 16;
            int contentWidth = rightPanelWidth - 36;
            
            int nameY = contentY + 14;
            int quoteY = nameY + 12;
            int sourceY = quoteY + 13;
            int storyY = sourceY + 32;
            int maxStoryHeight = rightPanelHeight - (storyY - rightPanelY) - 20;
            
            int totalStoryHeight = 0;
            for (String paragraph : ch.story()) {
                if (paragraph.isEmpty()) {
                    totalStoryHeight += 8;
                    continue;
                }
                List<String> wrappedLines = wrapText(paragraph, contentWidth - 8);
                totalStoryHeight += wrappedLines.size() * 11 + 3;
            }
            
            double maxStoryScroll = Math.max(0, totalStoryHeight - maxStoryHeight);
            
            this.storyScrollOffset = Mth.clamp(
                this.storyScrollOffset - verticalAmount * 20, 
                0, 
                maxStoryScroll
            );
            this.storyScrollTarget = this.storyScrollOffset;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void fillChamfered(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r > 0) {
            for (int dy = 0; dy < r && dy < h; dy++) {
                int dx = (int) Math.sqrt(r * r - (r - dy) * (r - dy));
                if (dx > 0) {
                    g.fill(x + r - dx, y + dy, x + r + dx, y + dy + 1, color);
                    g.fill(x + r - dx, y + h - dy - 1, x + r + dx, y + h - dy, color);
                }
            }
            for (int dx = 0; dx < r && dx < w; dx++) {
                int dy = (int) Math.sqrt(r * r - (r - dx) * (r - dx));
                if (dy > 0) {
                    g.fill(x + dx, y + r - dy, x + dx + 1, y + r + dy, color);
                    g.fill(x + w - dx - 1, y + r - dy, x + w - dx, y + r + dy, color);
                }
            }
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    private void drawDiamond(GuiGraphics g, int cx, int cy, int halfSize, int color) {
        if (halfSize <= 0) return;
        for (int i = 0; i < halfSize; i++) {
            float scale = (float) (halfSize - i) / halfSize;
            int w = Math.max(1, Math.round(scale * halfSize));
            g.fill(cx - w, cy - i, cx + w, cy - i + 1, color);
            if (i > 0) {
                g.fill(cx - w, cy + i, cx + w, cy + i + 1, color);
            }
        }
        g.fill(cx - halfSize, cy, cx + halfSize, cy + 1, color);
    }

    private void drawStringWithShadow(GuiGraphics g, String text, int x, int y, int color, boolean centered) {
        if (centered) {
            g.drawString(this.font, text, x - this.font.width(text) / 2, y, color, false);
        } else {
            g.drawString(this.font, text, x, y, color, false);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

    private static float approachFactor(float frameSeconds, float smoothness) {
        return 1.0f - (float) Math.exp(-smoothness * frameSeconds);
    }

    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }
}