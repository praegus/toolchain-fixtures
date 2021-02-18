package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.slim.ExecuteProgramTest;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class AwsCliFixture extends ExecuteProgramTest {
    private static final String AWS_CMD = "aws";
    private static final String S3 = "s3";
    private final String directory = new File(filesDir, "s3").getPath() + File.separator;

    public boolean copyToBucket(String file, String bucket) {
        checkBucketSyntax(bucket);
        String filePath = getFilePathFromWikiUrl(file);
        setAsArguments(new ArrayList<>(Arrays.asList(S3, "cp", filePath, bucket)));
        return execute(AWS_CMD);
    }

    public String downloadFromBucket(String file, String bucket) {
        checkBucketSyntax(bucket);
        File dlLocation = FileUtil.determineFilename(directory + file, "");
        setAsArguments(new ArrayList<>(Arrays.asList(S3, "cp", bucket + "/" + file, dlLocation.getAbsolutePath())));
        execute(AWS_CMD);
        return linkToFile(dlLocation);
    }

    public String listBucketContents(String bucket) {
        checkBucketSyntax(bucket);
        setAsArguments(new ArrayList<>(Arrays.asList(S3, "ls", bucket)));
        execute(AWS_CMD);
        return standardOut() + standardError();
    }

    public boolean listBucketContentsUntilOutputMatches(String bucket, String regex) {
        final String[] result = new String[1];
        result[0] = listBucketContents(bucket);
        return repeatUntil(new RepeatCompletion() {
            @Override
            public boolean isFinished() {
                return result[0].matches(regex);
            }

            @Override
            public void repeat() {
                result[0] = listBucketContents(bucket);
            }
        });
    }

    public boolean listBucketContentsUntilOutputDoesNotMatch(String bucket, String regex) {
        final String[] result = new String[1];
        result[0] = listBucketContents(bucket);
        return repeatUntil(new RepeatCompletion() {
            @Override
            public boolean isFinished() {
                return !result[0].matches(regex);
            }

            @Override
            public void repeat() {
                result[0] = listBucketContents(bucket);
            }
        });
    }


    private void checkBucketSyntax(String bucket) {
        if (!bucket.startsWith("s3://")) {
            throw new SlimFixtureException("Bucket location needs to include s3:// protocol prefix.");
        }
    }

}
