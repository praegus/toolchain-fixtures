package nl.praegus.fitnesse.slim.fixtures;

import com.google.gson.GsonBuilder;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Basic fixture to read values as String from excel sheets
 */
public class ExcelFileFixture extends SlimFixture {

    private final String directory = new File(filesDir, "xls").getPath() + File.separator;
    private String excelFile;
    private XSSFWorkbook workbook;

    /**
     * Select the xls file to read. Default directory is files/xls/ if it is not specified as a link/full path
     *
     * @param excelFile The name of path of the file
     */
    public void openExcelSheet(String excelFile) {
        this.excelFile = excelFile;
        try {
            workbook = new XSSFWorkbook(getFullName(excelFile));
        } catch (IOException e) {
            throw new SlimFixtureException("File: " + getFullName(excelFile) + "could not be opened");
        }
    }

    /**
     * get the String value of a cell
     *
     * @param row The row number (0-based)
     * @param col The column number (0-based)
     * @return The value of the cell, as a String
     */
    public String valueInRowColumn(int row, int col) {
        return valueInRowColumnInSheet(row, col, null);
    }

    /**
     * Get the String value of a cell in a given worksheet
     * Usage: | value in row | [row] | column | [column] | in sheet | [sheetName] |
     *
     * @param row       The row number (0-based)
     * @param col       The column number (0-based)
     * @param sheetName The name of the worksheet
     * @return The value of the cell, as a String
     */
    public String valueInRowColumnInSheet(int row, int col, String sheetName) {
        Cell cell = getCell(row, col, sheetName);
        return new DataFormatter().formatCellValue(cell);
    }

    private Cell getCell(int row, int col, String sheetName) {
        int sheetIndex = 0;
        if (null != sheetName) {
            sheetIndex = workbook.getSheetIndex(sheetName);
        }

        XSSFSheet sheet = workbook.getSheetAt(sheetIndex);

        XSSFRow sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            sheetRow = sheet.createRow(row);
        }

        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            cell = sheetRow.createCell(col);
        }
        return cell;
    }

    /**
     * Write a value to a cell
     * Usage: | write | [value] | to row | [row] | column | [column] |
     *
     * @param value The value to write
     * @param row   The row number (0-based)
     * @param col   The column number (0-based)
     */
    public void writeToRowColumn(String value, int row, int col) {
        writeToRowColumnInSheet(value, row, col, null);
    }

    /**
     * Write a value to a cell in a given worksheet
     * Usage: | write | [value] | to row | [row] | column | [column] | in sheet | [sheetName] |
     *
     * @param value     The value to write
     * @param row       The row number (0-based)
     * @param col       The column number (0-based)
     * @param sheetName The sheet to use
     */
    public void writeToRowColumnInSheet(String value, int row, int col, String sheetName) {
        try {
            Cell cell = getCell(row, col, sheetName);
            cell.setCellValue(value);

            String dummyFile = "tmp_outfile.xlsx";
            ensureParentExists(getFullName(dummyFile));
            FileOutputStream outFile = new FileOutputStream((getFullName(dummyFile)));
            workbook.write(outFile);
            outFile.close();

            Files.delete(Paths.get(getFullName(excelFile)));
            Files.move(Paths.get(getFullName(dummyFile)), Paths.get(getFullName(excelFile)));

        } catch (IOException e) {
            throw new SlimFixtureException("File: " + getFullName(excelFile) + "could not be found", e);
        }
    }


    private String getFullName(String filename) {
        String name;
        if (filename.startsWith(File.separator)
                || ":\\".equals(filename.substring(1, 3))) {
            name = filename;
        } else if (isFilesUrl(filename)) {
            name = getFilePathFromWikiUrl(filename);
        } else {
            name = directory + filename;
        }
        return cleanupPath(name);
    }

    private boolean isFilesUrl(String filename) {
        String url = getUrl(filename);
        return !filename.equals(url) && url.startsWith("files/");
    }

    private String cleanupPath(String fullPath) {
        return FilenameUtils.separatorsToSystem(fullPath);
    }

    protected void ensureParentExists(String fullName) {
        File f = new File(fullName);
        File parentFile = f.getParentFile();
        parentFile.mkdirs();
    }
}
