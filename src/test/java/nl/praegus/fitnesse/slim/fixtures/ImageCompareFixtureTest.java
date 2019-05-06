package nl.praegus.fitnesse.slim.fixtures;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageCompareFixtureTest {

    @Test
    public void when_pictueres_are_compared_a_comparison_map_is_created() throws IOException {
        ImageCompareFixture imageCompareFixture = new ImageCompareFixture();

        String result = imageCompareFixture.differencesBetweenAnd("src/test/resources/praegus-p.png", "src/test/resources/praegus-p.png");
        assertThat(result).contains("<div><a href=\"files/comparison-map-");
        assertThat(result).contains(".png\" target=\"_blank\"><img src=\"files/comparison-map-");
        assertThat(result).contains(".png\" title=\"toolchain-fixtures/FitNesseRoot/files/comparison-map-");
        assertThat(result).contains(".png\" height=\"400\"/></a></div>");


    }


}