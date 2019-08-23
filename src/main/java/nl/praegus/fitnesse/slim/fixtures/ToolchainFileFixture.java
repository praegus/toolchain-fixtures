package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.slim.FileFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ToolchainFileFixture extends FileFixture {

    /**
     * Get all filenames from the current directory.
     *
     * @return a list of file names
     */
    public List<String> filesInCurrentDirectory() {
        List<String> fileList = new ArrayList<>();
        File[] files = new File(getDirectory()).listFiles();
        if (null != files) {
            for (File file : files) {
                fileList.add(file.getName());
            }
        }
        return fileList;
    }

    /**
     * Find the first file with a name that matches a given regular expression
     *
     * @param pattern The regular expression pattern to match
     * @return The name of the first matching file in the current directory.
     */
    public String firstFileWithNameLike(String pattern) {
        Pattern regexPattern = Pattern.compile(pattern);

        List<String> files = filesInCurrentDirectory().stream()
                .filter(regexPattern.asPredicate())
                .collect(Collectors.toList());
        if (files.size() > 0) {
            return files.get(0);
        } else {
            throw new SlimFixtureException(false, "No file found that matches " + pattern);
        }
    }

    /**
     * Poll the current directory until no files are in it.
     * Use 'repeatAtMostTimes' and 'setRepeatInterval' to configure the repeat behaviour.
     *
     * @return true if the directory is empty, false if the maximum number of repeats are performed and the directory is not empty.
     */
    public boolean pollUntilCurrentDirectoryIsEmpty() {
        return repeatUntil(directoryIsEmptyCompletion());
    }

    private FunctionalCompletion directoryIsEmptyCompletion() {
        return new FunctionalCompletion(() -> filesInCurrentDirectory().size() == 0);
    }

    /**
     * Poll the current directory until a file matching the given pattern exists
     * Use 'repeatAtMostTimes' and 'setRepeatInterval' to configure the repeat behaviour.
     * Usage: | poll until file with name like | [pattern] | exists |
     *
     * @param pattern the regular expression pattern to match
     * @return true if a file is found matching the pattern, false if the maximum number of repeats are performed and no matching file exists.
     */
    public boolean pollUntilFileWithNameLikeExists(String pattern) {
        return repeatUntil(fileWithNameLikeExistsCompletion(pattern));
    }

    /**
     * Poll the current directory until no file matching the given pattern exists.
     * Use 'repeatAtMostTimes' and 'setRepeatInterval' to configure the repeat behaviour.
     * Usage: | poll until file with name like | [pattern] | does not exist |
     *
     * @param pattern The regular expression pattern to match
     * @return true if no file is found matching the pattern, false if the maximum number of repeats are performed and a matching file still exists.
     */
    public boolean pollUntilFileWithNameLikeDoesNotExist(String pattern) {
        return repeatUntilNot(fileWithNameLikeExistsCompletion(pattern));
    }

    private FunctionalCompletion fileWithNameLikeExistsCompletion(String pattern) {
        Pattern regexPattern = Pattern.compile(pattern);

        return new FunctionalCompletion(() -> filesInCurrentDirectory().stream()
                .filter(regexPattern.asPredicate())
                .collect(Collectors.toList()).size() > 0);
    }
}