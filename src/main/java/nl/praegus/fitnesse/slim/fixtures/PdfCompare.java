package nl.praegus.fitnesse.slim.fixtures;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PageArea;
import de.redsix.pdfcompare.PdfComparator;
import nl.hsac.fitnesse.fixture.slim.FileFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PdfCompare extends FileFixture {

    private String expected;
    private String actual;
    private CompareResult result;

    private List<PageArea> exclusions = new ArrayList<>();

    /**
     * Compare two pdf files. Set up using links to an expected pdf and the actual pdf
     *
     * @param expected Filename of the expected pdf file (can be a wiki file url or abslute path)
     * @param actual   Filename of the actual pdf file (can be a wiki file url or abslute path)
     */
    public PdfCompare(String expected, String actual) {
        String resultDir = new File(filesDir, "PdfCompare").getPath() + File.separator;
        setDirectory(resultDir);
        this.expected = getFullName(expected);
        this.actual = getFullName(actual);
    }

    /**
     * Compare the two PDF's using the current settings
     *
     * @return true if the pdf's are equal
     */
    public boolean compare() {
        try {
            PdfComparator comparator = new PdfComparator(this.expected, this.actual);
            for (PageArea exclusion : exclusions) {
                comparator = comparator.with(exclusion);
            }
            result = comparator.compare();
        } catch (IOException e) {
            throw new SlimFixtureException(false, e.getMessage());
        }

        return result.isEqual();
    }

    /**
     * Write the diff to a pdf file
     *
     * @return a link to the generated diff PDF
     */
    public String differencePdf() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        String diffFileName = "compare_" + dateFormat.format(date);
        String resultFile = getFullName(diffFileName);
        ensureParentExists(resultFile);
        if (null != result) {
            result.writeTo(resultFile);
        } else {
            throw new SlimFixtureException(false, "No compare result yet. Did you call compare?");
        }

        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", getWikiUrl(resultFile + ".pdf"), diffFileName);
    }

    /**
     * Exclude a page from the comparison
     *
     * @param page The page to exclude
     */
    public void excludePage(int page) {
        exclusions.add(new PageArea(page));
    }

    /**
     * Exclude an area (in px, at 300ppi) on any page (useful for page numbers, etc)
     *
     * @param area A comma-separated string: xStart, yStart, xEnd, yEnd
     */
    public void excludeAreaOnAnyPage(String area) {
        String[] coordinates = area.split(",");
        int xStart = Integer.valueOf(coordinates[0].trim());
        int yStart = Integer.valueOf(coordinates[1].trim());
        int xEnd = Integer.valueOf(coordinates[2].trim());
        int yEnd = Integer.valueOf(coordinates[3].trim());

        exclusions.add(new PageArea(xStart, yStart, xEnd, yEnd));
    }

    /**
     * Exclude an area (in px, at 300ppi) on a given page.
     * Usage: | exclude area | [area] | on page | [page] |
     *
     * @param area A comma-separated string: xStart, yStart, xEnd, yEnd
     * @param page The page to exclude the area on
     */
    public void excludeAreaOnPage(String area, int page) {
        String[] coordinates = area.split(",");
        int xStart = Integer.valueOf(coordinates[0].trim());
        int yStart = Integer.valueOf(coordinates[1].trim());
        int xEnd = Integer.valueOf(coordinates[2].trim());
        int yEnd = Integer.valueOf(coordinates[3].trim());

        exclusions.add(new PageArea(page, xStart, yStart, xEnd, yEnd));
    }

}
