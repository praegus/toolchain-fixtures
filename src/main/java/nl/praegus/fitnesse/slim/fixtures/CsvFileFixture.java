package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.slim.FileFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CsvFileFixture extends FileFixture {
    private String separator = ",";
    private String csvFile = "";

    /**
     * Define a separator character to use. Defaults to comma.
     *
     * @param separator
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * Set the file to work with
     *
     * @param csvFile
     */
    public void setCsvFile(String csvFile) {
        this.csvFile = csvFile;
    }

    /**
     * Get the value of a cell by matching a given value in another column
     * usage: | value of | [resultColumn] | in row where | [findByColumn] | is | [findByColumnValue] |
     *
     * @param resultColumn      The column to extract the value from
     * @param findByColumn      The column that contans the given value
     * @param findByColumnValue The value to search for
     * @return The value of 'resultColumn' in the row where 'findByColumn' has the value 'findByColumnValue'
     */
    public String valueOfInRowWhereIs(String resultColumn, String findByColumn, String findByColumnValue) {
        return valueOfInRowWhereIsIn(resultColumn, findByColumn, findByColumnValue, csvFile);
    }

    /**
     * Get the value of a cell by matching a given value in another column
     * usage: | value of | [resultColumn] | in row where | [findByColumn] | is | [findByColumnValue] | in | [filename] |
     *
     * @param resultColumn      The column to extract the value from
     * @param findByColumn      The column that contans the given value
     * @param findByColumnValue The value to search for
     * @param filename          The file to search in
     * @return The value of 'resultColumn' in the row where 'findByColumn' has the value 'findByColumnValue' in the given file.
     */
    public String valueOfInRowWhereIsIn(String resultColumn, String findByColumn, String findByColumnValue, String filename) {

        String result = "";
        ArrayList<String> lines = getLinesFromFile(filename);

        try {
            String[] columns = getColumns(lines);
            int resultColumnIndex = indexOfColumn(columns, resultColumn);

            if (resultColumnIndex >= 0) {
                String line = lineWhereIsIn(findByColumn, findByColumnValue, filename);
                String[] values = line.split(separator);
                result = values[resultColumnIndex];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("No row found where " + findByColumn + " is " + findByColumnValue);
        }

        return result;
    }

    /**
     * The value of 'reultColumn' in the nth row.
     * Usage: | value of | [resultColumn] | in row number | [number] |
     *
     * @param resultColumn
     * @param rowNumber
     * @return The value of the specified column in the specified row number
     */
    public String valueOfInRowNumber(String resultColumn, int rowNumber) {
        return valueOfInRowNumberIn(resultColumn, rowNumber, csvFile);
    }

    /**
     * The value of 'reultColumn' in the nth row in a given file
     * Usage: | value of | [resultColumn] | in row number | [number] | in | [filename] |
     *
     * @param resultColumn
     * @param rowNumber
     * @param filename
     * @return The value of the specified column in the specified row number in the given file
     */
    public String valueOfInRowNumberIn(String resultColumn, int rowNumber, String filename) {
        String result;
        ArrayList<String> lines = getLinesFromFile(filename);

        try {
            String[] columns = getColumns(lines);
            int resultColumnIndex = indexOfColumn(columns, resultColumn);
            String row = lines.get(rowNumber);
            String[] values = row.split(separator);
            result = values[resultColumnIndex];

        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("Row " + rowNumber + " has no field " + resultColumn);
        }
        return result;
    }

    /**
     * The name of the nth column
     *
     * @param column the column number to get the name from
     * @return the column name
     */
    public String nameOfColumn(int column) {
        return nameOfColumnIn(column, csvFile);
    }

    /**
     * The name of the nth column in the given file
     *
     * @param column   the column number to get the name from
     * @param filename The file to use
     * @return the column name
     */
    public String nameOfColumnIn(int column, String filename) {
        String result;
        ArrayList<String> lines = getLinesFromFile(filename);
        String[] columns = getColumns(lines);
        try {
            result = columns[column];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("No column at position " + column + " (Row 0 has " + columns.length + " columns)");
        }
        return result;
    }

    /**
     * Get the data from a row with a given number as a hashmap (key, val)
     *
     * @param row The row number
     * @return a map containing key value pairs (column names as keys)
     */
    public Map<String, String> dataInRow(int row) {
        return dataInRowIn(row, csvFile);
    }

    /**
     * Get the data from a row where a given column has a given value as a hashmap (key, val)
     *
     * @param column      The column name
     * @param lookupValue The value to find in the given column
     * @return a map containing key value pairs (column names as keys)
     */
    public Map<String, String> dataInRowWhereIs(String column, String lookupValue) {
        return dataInRowWhereIsIn(column, lookupValue, csvFile);
    }

    /**
     * Get the data from a row where a given column has a given value in the given file as a hashmap (key, val)
     *
     * @param column      The column name
     * @param lookupValue The value to find in the given column
     * @param filename    The file to use
     * @return a map containing key value pairs (column names as keys)
     */
    public Map<String, String> dataInRowWhereIsIn(String column, String lookupValue, String filename) {
        Map<String, String> data = new HashMap<>();
        try {
            ArrayList<String> lines = getLinesFromFile(filename);
            String[] columns = getColumns(lines);
            int columnIndex = indexOfColumn(columns, column);

            if (columnIndex >= 0) {
                for (String line : lines) {
                    String[] values = line.split(separator);
                    if (values[columnIndex].equals(lookupValue)) {
                        String[] keys = lines.get(0).split(separator);
                        for (int j = 0; j < keys.length; j++) {
                            data.put(keys[j], values[j]);
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("No row found where " + column + " is " + lookupValue);
        }
        return data;
    }

    /**
     * Get the data from a row with a given number in a given file as a hashmap (key, val)
     *
     * @param row      The row number
     * @param filename The file to use
     * @return a map containing key value pairs (column names as keys)
     */
    public Map<String, String> dataInRowIn(int row, String filename) {
        Map<String, String> data = new HashMap<>();
        ArrayList<String> lines = getLinesFromFile(filename);
        String[] keys = lines.get(0).split(separator);
        String[] values = lines.get(row).split(separator);
        for (int j = 0; j < keys.length; j++) {
            data.put(keys[j], values[j]);
        }
        return data;
    }

    public int numberOfLines() {
        return numberOfLinesIn(csvFile);
    }

    public int numberOfLinesIn(String fileName) {
        return getLinesFromFile(fileName).size();
    }

    public int numberOfLinesWhereIs(String column, String lookupValue) {
        return numberOfLinesWhereIsIn(column, lookupValue, csvFile);
    }

    public int numberOfLinesWhereIsIn(String column, String lookupValue, String filename) {
        int result = 0;
        try {
            ArrayList<String> lines = getLinesFromFile(filename);
            String[] columns = getColumns(lines);
            int columnIndex = indexOfColumn(columns, column);

            if (columnIndex >= 0) {
                for (String line : lines) {
                    String[] values = line.split(separator);
                    if (values[columnIndex].equals(lookupValue)) {
                        result++;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("No row found where " + column + " is " + lookupValue);
        }
        return result;
    }

    private ArrayList<String> getLinesFromFile(String filename) {
        String fullName = getFullName(filename);
        ensureParentExists(fullName);
        File file = new File(fullName);
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new SlimFixtureException(e);
        }
        return lines;
    }

    private String lineWhereIsIn(String findByColumn, String findByColumnValue, String fileName) {
        ArrayList<String> lines = getLinesFromFile(fileName);
        String result = "";
        try {
            String[] columns = getColumns(lines);
            int findColumnIndex = indexOfColumn(columns, findByColumn);

            if (findColumnIndex >= 0) {
                lines.remove(0);
                for (String line : lines) {
                    String[] values = line.split(separator);
                    if (values[findColumnIndex].equals(findByColumnValue)) {
                        result = line;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SlimFixtureException("No row found where " + findByColumn + " is " + findByColumnValue);
        }
        return result;
    }

    private String[] getColumns(ArrayList<String> lines) {
        return lines.get(0).split(separator);
    }

    private int indexOfColumn(String[] columns, String columnName) {
        int columnIndex = -1;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equals(columnName)) {
                columnIndex = i;
            }
        }
        return columnIndex;
    }
}
