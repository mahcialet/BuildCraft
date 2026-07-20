package buildcraft.core.client.guide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Loads the historical external BuildCraftGuide markdown pack from client resources. */
public final class GuideRepository {
    private static final String ROOT = "compat/buildcraft/guide/en_us/";
    private static final Pattern CHAPTER = Pattern.compile("<chapter\\s+name=\"([^\"]+)\"\\s*/>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    public record Page(String id, String title, String category, List<String> paragraphs, String searchable) { }

    private GuideRepository() { }

    public static List<Page> load() {
        List<Page> pages = new ArrayList<>();
        Minecraft.getInstance().getResourceManager().listResources("compat/buildcraft/guide",
            id -> id.getPath().startsWith(ROOT) && id.getPath().endsWith(".md")
        ).forEach((id, resource) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.open(), StandardCharsets.UTF_8))) {
                String raw = reader.lines().reduce("", (left, right) -> left + right + "\n");
                pages.add(parse(id, raw));
            } catch (IOException ignored) {
                // A broken optional resource-pack page must not prevent the guide from opening.
            }
        });
        pages.sort(Comparator.comparing(Page::category).thenComparing(Page::title));
        return List.copyOf(pages);
    }

    private static Page parse(Identifier source, String raw) {
        String relative = source.getPath().substring(ROOT.length(), source.getPath().length() - 3);
        String id = source.getNamespace() + ":" + relative;
        String category = relative.contains("/") ? relative.substring(0, relative.indexOf('/')) : "general";
        Matcher chapter = CHAPTER.matcher(raw);
        String title = chapter.find() ? chapter.group(1) : humanName(relative.substring(relative.lastIndexOf('/') + 1));

        String cleaned = raw
            .replace("<lore>", "").replace("</lore>", "")
            .replaceAll("(?s)<no_lore>.*?</no_lore>", "")
            .replaceAll("<new_page\\s*/>", "\n\n")
            .replaceAll("<chapter\\s+name=\"([^\"]+)\"\\s*/>", "\n\n$1\n")
            .replaceAll("<recipes(?:_usages)?[^>]*/>", "\n[Recipes]\n")
            .replaceAll("<image[^>]*/>", "\n[Image]\n")
            .replaceAll("<note\\s+id=\"([^\"]+)\"\\s*>", "\nNote: $1\n")
            .replace("</note>", "")
            .replaceAll("<(?:bold|italic|underline|blue|green|red)>", "")
            .replaceAll("</(?:bold|italic|underline|blue|green|red)>", "");
        cleaned = TAG.matcher(cleaned).replaceAll("");
        List<String> paragraphs = new ArrayList<>();
        for (String value : cleaned.split("\\n\\s*\\n|\\R")) {
            String text = value.trim().replaceAll("\\s+", " ");
            if (!text.isEmpty()) paragraphs.add(text);
        }
        return new Page(id, title, source.getNamespace() + " / " + category,
            List.copyOf(paragraphs), (title + " " + id + " " + cleaned).toLowerCase(Locale.ROOT));
    }

    private static String humanName(String value) {
        String[] words = value.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }
}
