package nl.praegus.fitnesse.junit.testsystemlisteners.util;

import nl.hsac.fitnesse.fixture.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputChunkParser {

    private final String NEWLINE = System.getProperty("line.separator");

    private int collapsedSectionDepth = 0;
    private boolean hideOutput = false;
    private String collapsedType = "";

    public String rewriteHashTables(String html) {
        Pattern hashTablePattern = Pattern.compile("<table class=\"hash_table\">(.+?)</table>");
        Matcher m = hashTablePattern.matcher(html);
        while (m.find()) {
            String hashTableContent = m.group(1);
            hashTableContent = hashTableContent.replaceAll("\t", "")
                    .replaceAll("<tr.*?><td.*?>(.+?)</td><td.*?>(.+?)</td></tr>", "$1 : $2, ")
                    .replaceAll("^", "!{ ")
                    .replaceAll("$", " }")
                    .replaceAll(",  }", " }");
            html = m.replaceAll(hashTableContent);
        }

        return html;
    }

    public String embedImages(String html) {

        final Pattern imgPattern = Pattern.compile("<img(\\s+.*?)?\\s+src=\"(.*?)\".*?/>", Pattern.CASE_INSENSITIVE);
        html = html.replaceAll("<a.+?>(.+?)</a>", "$1");
        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String src = imgMatcher.group(2);
            String root = Environment.getInstance().getFitNesseRootDir();
            String img = root + "/" + src;
            File imageFile = new File(img);
            html = imgMatcher.replaceAll("<img src=\"data:image/png;base64," + encodeFile(imageFile) + "\" width=\"200\" onClick=\"openImage(this)\">");
        }
        return html;
    }

    private String encodeFile(File file) {
        String base64Image = "";
        try (FileInputStream imageInFile = new FileInputStream(file)) {
            // Reading a Image file from file system
            byte imageData[] = new byte[(int) file.length()];
            imageInFile.read(imageData);
            base64Image = Base64.getEncoder().encodeToString(imageData);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
        return base64Image;
    }

    public String formatHtmlForConsole(String html) {
        return html
                .replaceAll("\t", "")
                .replaceAll("<h[0-9][^>]*>(.+?)</h[0-9]>", ConsoleColors.BLUE_BOLD_BRIGHT + "$1" + "\r\n" + ConsoleColors.RESET)
                .replaceAll("<span class=\"meta\">(.+?)</span>", ConsoleColors.GREEN + "$1" + ConsoleColors.RESET)
                .replaceAll("<br/>", "\r\n")
                .replaceAll("\\s*<li>(.+?)</li>", "* $1 \r\n")
                .replaceAll("</*ul>\\s*", "\r\n")
                .replaceAll("<i>(.+?)</i>", ConsoleColors.WHITE_BOLD + "$1" + ConsoleColors.RESET)
                .replaceAll("<b>(.+?)</b>", ConsoleColors.WHITE_BOLD + "$1" + ConsoleColors.RESET)
                .replaceAll("<p class=\"note\">(.+?)</p>", ConsoleColors.BLACK_BRIGHT + "$1" + ConsoleColors.RESET)
                .replaceAll("<td(.*?)class=\"pass.*?\">(.+?)</td>", "<td$1>#p$2#/p</td>")
                .replaceAll("<td(.*?)class=\"fail.*?\">(.+?)</td>", "<td$1>#f$2#/f</td>")
                .replaceAll("<td(.*?)class=\"error.*?\">(.+?)</td>", "<td$1>#e$2#/e</td>")
                .replaceAll("<td(.*?)class=\"ignore.*?\">(.+?)</td>", "<td$1>#i$2#/i</td>")
                .replaceAll("<span class=\"diff\">(.+?)</span>", "$1")
                .replaceAll("<span class=\"pass\">(.+?)</span>", "#p$1#/p")
                .replaceAll("<span class=\"fail\">(.+?)</span>", "#f$1#/f")
                .replaceAll("<span class=\"error\">(.+?)</span>", "#e$1#/e")
                .replaceAll("<span class=\"ignore\">(.+?)</span>", "#i$1#/i")
                .replaceAll("#/f#f", "");
    }

    public String sanitizeRemainingHtml(String html) {
        return html
                .replaceAll("<tr.*?>", "")
                .replaceAll("</tr>", "|" + "\r\n")
                .replaceAll("<td.*?>", "|")
                .replaceAll("</td>", "")
                .replaceAll("<table>", "")
                .replaceAll("</table>", "")
                .replaceAll("<div.*?>", "")
                .replaceAll("</div>", "")
                .replaceAll("<.*? href=\"(.+?)\".*?>", " $1 ")
                .replaceAll("<.*?>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("(?i)\\|\\|(scenario|table template)", "|$1")
                .replaceAll("(?m)^\\|\\s*$", "|#endscenario#|");
    }

    public String applyConsoleColoring(String html) {
        return WikitextJSFormatter.formatWikiText(html)
                .replaceAll("(?i)(\\|scenario|\\|table template)", ConsoleColors.BLUE_BRIGHT + "$1" + ConsoleColors.RESET)
                .replaceAll("#endscenario#", ConsoleColors.BLUE_BRIGHT + "end scenario" + ConsoleColors.RESET + " ")
                .replaceAll("#i(.+?)#/i", ConsoleColors.BLUE_BACKGROUND + ConsoleColors.BLACK + "$1" + ConsoleColors.RESET + "     ")
                .replaceAll("#p(.+?)#/p", ConsoleColors.GREEN + "$1" + ConsoleColors.RESET + "     ")
                .replaceAll("#f(.+?)#/f", ConsoleColors.RED + "$1" + ConsoleColors.RESET + "     ")
                .replaceAll("#e(.+?)#/e", ConsoleColors.BLACK_BRIGHT + ConsoleColors.YELLOW_BACKGROUND_BRIGHT + "$1" + ConsoleColors.RESET + "     ")
                .replaceAll("#f", "  ")
                .replaceAll("#/f", "   ");
    }

    public String filterCollapsedSections(String html) {
        //remove oneline close/open row
        html = html.replaceAll("</tr><tr", "</tr>" + NEWLINE + "</tr");
        Pattern startDiv = Pattern.compile("<div.*?>");
        Pattern endDiv = Pattern.compile("</div>");
        Pattern startRow = Pattern.compile("<tr.*?>");
        Pattern endRow = Pattern.compile("</tr>");
        Pattern collapsedDiv = Pattern.compile("<div class=\".*?closed.*?\"");
        Pattern collapsedRow = Pattern.compile("<tr class=\".*?closed-detail.*?\"");

        String lines[] = html.split(NEWLINE);
        String result = "";

        for (String line : lines) {
            Matcher divMatcher = collapsedDiv.matcher(line);
            Matcher rowMatcher = collapsedRow.matcher(line);
            if (!hideOutput && divMatcher.find()) {
                hideOutput = true;
                collapsedType = "div";
                result += line.substring(0, divMatcher.start());
            }
            if (!hideOutput && rowMatcher.find()) {
                hideOutput = true;
                collapsedType = "tr";
                result += line.substring(0, rowMatcher.start());
            }
            if (hideOutput) {
                Pattern startPattern = startDiv;
                Pattern endPattern = endDiv;
                if (collapsedType.equals("tr")) {
                    startPattern = startRow;
                    endPattern = endRow;
                }
                Matcher startMatcher = startPattern.matcher(line);
                while (startMatcher.find()) {
                    collapsedSectionDepth++;
                }
                Matcher endMatcher = endPattern.matcher(line);
                while (endMatcher.find()) {
                    collapsedSectionDepth--;
                    if (collapsedSectionDepth == 0) {
                        hideOutput = false;
                        line = line.substring(endMatcher.end());
                    }
                }
            }
            if (!hideOutput) {
                if (!line.isEmpty()) {
                    result += line.trim();
                }
            }
        }
        return result;
    }

    public String printSummary(String test, String summary) {
        final String HORIZONTAL_LINE =
                "-------------------------------------------------------------------------------------------------------------------------";

        return "\r\n" + HORIZONTAL_LINE + "\r\n" +
                ConsoleColors.YELLOW +
                " Summary of " + test + ": " + summary +
                ConsoleColors.RESET +
                "\r\n" + HORIZONTAL_LINE + "\r\n";
    }
}
