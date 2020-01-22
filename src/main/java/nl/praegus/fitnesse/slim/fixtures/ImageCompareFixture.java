package nl.praegus.fitnesse.slim.fixtures;

import com.galenframework.rainbow4j.ComparisonOptions;
import com.galenframework.rainbow4j.ImageCompareResult;
import com.galenframework.rainbow4j.Rainbow4J;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

/**
 * Fixture to compare two images. Accepted file types: PNG, BMP, JPEG, GIF. Extends Slim fixture from
 * nl.hsac.fitnesse.fixture.slim package. Uses the rainbow4j library from the Galen project to perform image compare.
 */
public class ImageCompareFixture extends SlimFixture {
    private final ComparisonOptions comparisonOptions = new ComparisonOptions();
    private double acceptedDifferencePercentage = 0.0;
    private String comparisonMapFileFormat = "png";
    private List<Rectangle> ignoreRegions = new ArrayList<>();

    public void setAcceptedDifferencePercentage(double acceptedDifferencePercentage) {
        this.acceptedDifferencePercentage = acceptedDifferencePercentage;
    }

    public void setTolerance(int tolerance) {
        comparisonOptions.setTolerance(tolerance);
    }

    public void setComparisonMapFileFormat(String comparisonMapFileFormat) {
        this.comparisonMapFileFormat = comparisonMapFileFormat;
    }

    /**
     * Returns an image showing a heatmap of differences between two given images. Images given can be of PNG, BMP, JPEG
     * or GIF file format. The file format of the compared images might differ.
     *
     * @param baselineImagePath path of the image to be used as baseline to test an image against. Format is
     *                          url: http://files/example.png or
     *                          relative path: files/example.png or
     *                          absolute path : C:\Projects\someProject\wiki\FitNesseRoot\files\example.png
     * @param testImagePath     the image to test against the given baseline
     * @return HTML link to the generate heatmap
     * @throws IOException when image can not be saved
     */
    public String differencesBetweenAnd(String baselineImagePath, String testImagePath) throws IOException {
        ImageCompareResult compareResult = compareImages(baselineImagePath, testImagePath);
        return getImageLink(saveComparisonMap(compareResult));
    }

    /**
     * Boolean indicating if two given images are visually equal (depending on set tolerance and accepted
     * difference percentage)
     *
     * @param baselineImagePath path of the image to be used as baseline to test an image against. Format is
     *                          url: http://files/example.png or
     *                          relative path: files/example.png or
     *                          absolute path : C:\Projects\someProject\wiki\FitNesseRoot\files\example.png
     * @param testImagePath     the image to test against the given baseline
     * @return true when the two given images are visually equal
     * @throws IOException
     */
    public boolean imageIsEqualTo(String baselineImagePath, String testImagePath) throws IOException {
        boolean result = false;
        ImageCompareResult compareResult = compareImages(baselineImagePath, testImagePath);
        if (compareResult.getPercentage() <= acceptedDifferencePercentage) {
            result = true;
        }
        return result;
    }

    /**
     * Add a region to ignore.
     * A region is formatted as x, y, width, height in pixels. So 0,0,20,100 will ignore a region of 20x100px in the top left corner of the image
     * @param region a comma separated list of integers (x,y,width,height)
     */
    public void addExcludeRegion(String region) {
        String[] regionInfo = region.split(",");
        if(regionInfo.length != 4) {
            throw new SlimFixtureException(false, "A region consists of exactly 4 integers: x, y, width, height");
        }
        try {
            Rectangle newRegion = new Rectangle(Integer.parseInt(regionInfo[0].trim()),
                    Integer.parseInt(regionInfo[1].trim()),
                    Integer.parseInt(regionInfo[2].trim()),
                    Integer.parseInt(regionInfo[3].trim()));
            ignoreRegions.add(newRegion);
            comparisonOptions.setIgnoreRegions(ignoreRegions);
        } catch (NumberFormatException e) {
            throw new SlimFixtureException(false, "A region consists of exactly 4 integers: x, y, width, height", e);
        }

    }

    /**
     * Compares two given images.
     *
     * @param baselineImagePath image to use as baseline for comparison
     * @param testImagePath     image to test against baseline
     * @return comparison result object
     * @throws IOException if images can not be read
     */
    private ImageCompareResult compareImages(String baselineImagePath, String testImagePath) throws IOException {
        return Rainbow4J.compare(loadImageFromPath(baselineImagePath), loadImageFromPath(testImagePath), comparisonOptions);
    }

    /**
     * Creates BufferedImage from given path
     *
     * @param imagePath the path to the image to load
     * @return BufferedImage from image found on path
     * @throws IOException if image can not be read
     */
    private BufferedImage loadImageFromPath(String imagePath) throws IOException {
        String baselineImagePath = getFilePathFromWikiUrl(imagePath);
        BufferedImage image = Rainbow4J.loadImage(baselineImagePath);
        return image;
    }

    /**
     * Saves comparison map of a given image compare result to disk
     *
     * @param compareResult image compare result object
     * @return path of the saved comparison map
     * @throws IOException when saving fails
     */
    private String saveComparisonMap(ImageCompareResult compareResult) throws IOException {
        BufferedImage diffMap = compareResult.getComparisonMap();
        String imageName = getComparisonMapName();
        ImageIO.write(diffMap, "png", new File(imageName));
        return imageName;
    }

    /**
     * Returns unique name for generated comparison map based on current time.
     *
     * @return name for comparison map
     */
    private String getComparisonMapName() {
        String imageCreationTimestamp = new SimpleDateFormat("ddHHmmSSS").format(new java.util.Date());
        return String.format("%s%scomparison-map-%s.%s", filesDir, File.separator, imageCreationTimestamp, comparisonMapFileFormat);
    }

    /**
     * Returns HTML link to given image where height of the image is set to 400px
     *
     * @param imagePath
     * @return HTML link to given image
     */
    private String getImageLink(String imagePath) {
        return String.format("<div><a href=\"%1$s\" target=\"_blank\"><img src=\"%1$s\" title=\"%2$s\" height=\"%3$s\"/></a></div>",
                getWikiUrl(imagePath), imagePath, "400");
    }
}
