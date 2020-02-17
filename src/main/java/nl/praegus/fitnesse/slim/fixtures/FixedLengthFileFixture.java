package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.slim.FileFixture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FixedLengthFileFixture extends FileFixture {
    private List<String> lines;

    public FixedLengthFileFixture(String fileName) {
        loadFile(fileName);
    }

    public void loadFile(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            Stream<String> stream = Files.lines(Paths.get(getFilePathFromWikiUrl(fileName)));
            lines = stream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        this.lines = lines;
    }

    public String valueOnPositionWithLengthOnLine(int startPos, int length, int lineNumber) {
        String line = lines.get(lineNumber-1);
        return line.substring(startPos-1, startPos + length -1);
    }

    public String trimmedValueOnPositionWithLengthOnLine(int startPos, int length, int lineNumber) {
        return valueOnPositionWithLengthOnLine(startPos, length, lineNumber).trim();
    }

}
