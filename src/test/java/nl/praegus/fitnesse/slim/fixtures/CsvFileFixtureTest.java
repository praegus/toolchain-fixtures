package nl.praegus.fitnesse.slim.fixtures;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CsvFileFixtureTest {

    @Test
    public void when_csv_file_is_found_a_row_can_be_retreived(){
        CsvFileFixture fileFixture = new CsvFileFixture();

        fileFixture.setCsvFile("test.csv");

        Map<String, String> result = fileFixture.dataInRow(1);

        assertThat(result.get("kolom1")).isEqualTo("inhoud1");
    }

    @Test
    public void when_csv_file_is_found_a_value_from_a_row_can_be_retreived(){
        CsvFileFixture fileFixture = new CsvFileFixture();

        fileFixture.setCsvFile("test.csv");

        String result = fileFixture.valueOfInRowNumberIn("kolom1" , 1, "test.csv");

        assertThat(result).isEqualTo("inhoud1");
    }

    @Test
    public void when_csv_file_is_found_a_value_from_a_row_can_be_retreived_through_another_row(){
        CsvFileFixture fileFixture = new CsvFileFixture();

        fileFixture.setCsvFile("test.csv");

        String result = fileFixture.valueOfInRowWhereIsIn("kolom1" , "kolom2","inhoud2", "test.csv");

        assertThat(result).isEqualTo("inhoud1");
    }

}