package nl.praegus.fitnesse.junit.listeners;

import com.epam.reportportal.junit.IListenerHandler;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.reportportal.service.ReportPortalService;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.inject.Inject;
import fitnesse.junit.FitNessePageAnnotation;
import fitnesse.wiki.WikiPage;
import nl.hsac.fitnesse.fixture.Environment;
import nl.praegus.fitnesse.junit.testsystemlisteners.StandaloneHtmlListener;
import org.apache.commons.io.FilenameUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.reportportal.listeners.ListenersUtils.handleException;

public class ToolchainRunHandler implements IListenerHandler {
    public final static String API_BASE = "/reportportal-ws/api/v1";

    private final Logger logger = LoggerFactory.getLogger(ToolchainRunHandler.class);
    private ToolchainRunningContext context;
    private ReportPortalService reportPortalService;
    private String launchName;
    private Set<String> tags;
    private Mode launchRunningMode;

    private static final String SCREENSHOT_EXT = "png";
    private static final String PAGESOURCE_EXT = "html";
    private static final Pattern SCREENSHOT_PATTERN = Pattern.compile("href=\"([^\"]*." + SCREENSHOT_EXT + ")\"");
    private static final Pattern PAGESOURCE_PATTERN = Pattern.compile("href=\"([^\"]*." + PAGESOURCE_EXT + ")\"");
    private static List<Pattern> patterns = new ArrayList<>();
    private final Environment hsacEnvironment = Environment.getInstance();

    @Inject
    ToolchainRunHandler(ListenerParameters parameters, ToolchainRunningContext runningContext,
                                  BatchedReportPortalService reportPortalService) {
        this.launchName = parameters.getLaunchName();
        this.tags = parameters.getTags();
        this.launchRunningMode = parameters.getMode();
        this.context = runningContext;
        this.reportPortalService = reportPortalService;
        patterns.add(SCREENSHOT_PATTERN);
        patterns.add(PAGESOURCE_PATTERN);
    }

    @Override
    public void startLaunch() {
        if (null == context.getLaunchId() || context.getLaunchId().isEmpty()) {
            StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
            startLaunchRQ.setName(launchName);
            startLaunchRQ.setStartTime(Calendar.getInstance().getTime());
            startLaunchRQ.setTags(tags);
            startLaunchRQ.setMode(launchRunningMode);
            EntryCreatedRS rs;
            try {
                rs = reportPortalService.startLaunch(startLaunchRQ);
                context.setLaunchId(rs.getId());
            } catch (Exception e) {
                handleException(e, logger, "Unable start the launch: '" + launchName + "'");
            }
        }
    }

    @Override
    public void stopLaunch() {
        if (!Strings.isNullOrEmpty(context.getLaunchId())) {
            FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
            finishExecutionRQ.setEndTime(Calendar.getInstance().getTime());
            try {
                reportPortalService.finishLaunch(context.getLaunchId(), finishExecutionRQ);
            } catch (Exception e) {
                handleException(e, logger, "Unable finish the launch: '" + launchName + "'");
            }
        }
    }

    @Override
    public void startTestMethod(Description description) {
        String testName = getTestName(description);
        if(testName.equalsIgnoreCase("suitesetup") || testName.equalsIgnoreCase("suiteteardown")) {
            return;
        }
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(testName);
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType("TEST");
        FitNessePageAnnotation pageAnn = description.getAnnotation(FitNessePageAnnotation.class);
        if (pageAnn != null) {
            WikiPage page = pageAnn.getWikiPage();
            rq.setTags(getTags(page));
        }
        rq.setLaunchId(context.getLaunchId());
        String suiteId = context.getSuiteId(getSuiteName(description));
        EntryCreatedRS rs;
        try {
            rs = reportPortalService.startTestItem(suiteId, rq);
            context.addTest(getFullTestName(description), rs.getId());
            ReportPortalListenerContext.setRunningNowItemId(rs.getId());
        } catch (Exception e) {
            handleException(e, logger, "Unable start test: '" + getFullTestName(description) + "'");
        }
    }

    @Override
    public void stopTestMethod(Description description) {
        String testName = getTestName(description);
        if(testName.equalsIgnoreCase("suitesetup") || testName.equalsIgnoreCase("suiteteardown")) {
            return;
        }

        sendPlainHtmlLog(description);

        ReportPortalListenerContext.setRunningNowItemId(null);
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        String status = context.getStatus(getFullTestName(description));
        rq.setStatus((status == null || status.equals("")) ? Statuses.PASSED : status);
        try {
            String fullTestName = getFullTestName(description);
            reportPortalService.finishTestItem(context.getTestId(fullTestName), rq);
            context.addFinishedTest(fullTestName);
        } catch (Exception e) {
            handleException(e, logger, "Unable finish test: '" + getFullTestName(description) + "'");
        }
    }

    @Override
    public void markCurrentTestMethod(Description description, String status) {
        if (description.isSuite()) {
            return;
        }
        context.addStatus(getFullTestName(description), status);
    }

    @Override
    public void initSuiteProcessor(Description description) {}

    @Override
    public void startSuiteIfRequired(Description description) {
        String testName = getTestName(description);
        if(testName.equalsIgnoreCase("suitesetup") || testName.equalsIgnoreCase("suiteteardown")) {
            return;
        }
        String suiteName = getSuiteName(description);
        String suiteId = context.getSuiteId(suiteName);
        if (suiteId == null) {
            StartTestItemRQ rq = new StartTestItemRQ();
            rq.setLaunchId(context.getLaunchId());
            rq.setName(suiteName);
            rq.setType("SUITE");
            rq.setStartTime(Calendar.getInstance().getTime());
            EntryCreatedRS rs;
            try {
                rs = reportPortalService.startRootTestItem(rq);
                context.addSuite(suiteName, rs.getId());

            } catch (Exception e) {
                handleException(e, logger, "Unable start test suite: '" + suiteName + "'");
            }
        }
    }



    @Override
    public void stopSuiteIfRequired(Description description) {
        String testName = getTestName(description);
        if(testName.equalsIgnoreCase("suitesetup") || testName.equalsIgnoreCase("suiteteardown")) {
            return;
        }
        final String suiteName = getSuiteName(description);
            final String suiteId = context.getSuiteId(suiteName);
            final FinishTestItemRQ rq = new FinishTestItemRQ();
            rq.setEndTime(Calendar.getInstance().getTime());
            try {
                reportPortalService.finishTestItem(suiteId, rq);
            } catch (Exception e) {
                handleException(e, logger, "Unable finish test suite: '" + suiteName + "'");
            }
    }

    void stopAllSuites() {
        for(String suiteId : context.getAllSuiteIds()) {
            stopSuite(suiteId);
        }
    }

    private void stopSuite(String suiteId) {
        final FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        try {
            reportPortalService.finishTestItem(suiteId, rq);
        } catch (Exception e) {
            handleException(e, logger, "Unable finish test suite: '" + context.getSuiteName(suiteId) + "'");
        }
    }

    @Override
    public void stopTestIfRequired(Description description) {
        stopTestMethod(description);
    }

    @Override
    public void starTestIfRequired(Description description) {
        startTestMethod(description);
    }

    @Override
    public void handleTestSkip(Description description) {
        if (description.isTest()) {
            return;
        }
        startSuiteIfRequired(description);
        StartTestItemRQ startRQ = new StartTestItemRQ();
        startRQ.setStartTime(Calendar.getInstance().getTime());
        startRQ.setName(getTestName(description));
        startRQ.setType("TEST");
        startRQ.setLaunchId(context.getLaunchId());
        String suiteName = getSuiteName(description);

        try {
            EntryCreatedRS rs = reportPortalService.startTestItem(context.getSuiteId(suiteName), startRQ);
            FinishTestItemRQ finishRQ = new FinishTestItemRQ();
            finishRQ.setStatus(Statuses.FAILED);
            finishRQ.setEndTime(Calendar.getInstance().getTime());
            reportPortalService.finishTestItem(rs.getId(), finishRQ);
            context.addFinishedTest(getFullTestName(description));
        } catch (Exception e) {
            handleException(e, logger, "Unable skip test: '" + getTestName(description) + "'");
        }
    }

    @Override
    public void addToFinishedMethods(Description description) {
        context.addFinishedTest(getFullTestName(description));
    }

    @Override
    public void clearRunningItemId() {
        ReportPortalListenerContext.setRunningNowItemId(null);
    }

    @Override
    public void sendReportPortalMsg(Failure result) {
        SaveLogRQ saveLogRQ = new SaveLogRQ();
        if (result.getException() != null) {
            saveLogRQ.setMessage(
                    "Exception: " + result.getException().getMessage() + System.getProperty("line.separator") + this.getStackTraceString(
                            result.getException()));
        } else {
            saveLogRQ.setMessage("Just exception (contact dev team)");
        }
        saveLogRQ.setLogTime(Calendar.getInstance().getTime());
        saveLogRQ.setTestItemId(context.getTestId(getFullTestName(result.getDescription())));
        saveLogRQ.setLevel("ERROR");
        try {
            reportPortalService.log(saveLogRQ);
        } catch (Exception e1) {
            handleException(e1, logger, "Unable to send message to Report Portal");
        }
        processAttachmentsInFailure(result);
    }

    private void sendPlainHtmlLog(Description description) {
        ByteSource attachmentSource = ByteSource.wrap(StandaloneHtmlListener.output.toString().getBytes());
        SaveLogRQ.File attachmentFile = new SaveLogRQ.File();
        attachmentFile.setContent(attachmentSource);
        attachmentFile.setName("Plain HTML Report");
        sendAttachment("Plain HTML Report", "INFO", context.getTestId(getFullTestName(description)), attachmentFile);
    }

    private String getStackTraceString(Throwable e) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < e.getStackTrace().length; i++) {
            result.append(e.getStackTrace()[i]);
            result.append(System.getProperty("line.separator"));
        }
        return result.toString();
    }

    private String getTestName(Description description) {
        String[] testNameParts = getFullTestName(description).split("\\.");
        return testNameParts[testNameParts.length - 1];
    }

    private String getSuiteName(Description description) {
        String[] testNameParts = getFullTestName(description).split("\\.");
        return testNameParts[testNameParts.length - 2];
    }

    private String getFullTestName(Description description) {
        return description.getDisplayName().replaceAll("\\(.+\\)", "");
    }

    void attachWikiPage(Description description) {
        FitNessePageAnnotation pageAnn = description.getAnnotation(FitNessePageAnnotation.class);
        if (pageAnn != null) {
            WikiPage page = pageAnn.getWikiPage();
            String pageContent = page.getData().getContent()
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;");

            ByteSource attachmentSource = ByteSource.wrap(pageContent.getBytes());
            SaveLogRQ.File attachmentFile = new SaveLogRQ.File();
            attachmentFile.setContent(attachmentSource);
            attachmentFile.setName("Wiki Content");
            sendAttachment("Wiki Content", "INFO", context.getTestId(getFullTestName(description)), attachmentFile);
        }
    }

    private void sendAttachment(final String message, final String level, final String testItem, final SaveLogRQ.File file) {
        SaveLogRQ rq = new SaveLogRQ();
        rq.setMessage(message);
        rq.setLevel(level);
        rq.setTestItemId(testItem);
        rq.setLogTime(Calendar.getInstance().getTime());
        if(null != file) {
            rq.setFile(file);
        }
        try {
            reportPortalService.log(rq);
        } catch (Exception e1) {
            handleException(e1, logger, "Unable to send attachment to Report Portal");
        }
    }

    private void processAttachmentsInFailure(Failure result) {
        Throwable ex = result.getException();
        Set<String> attachments = new HashSet<>();
        if (null != ex.getMessage()) {
            for (Pattern pattern : patterns) {
                Matcher patternMatcher = pattern.matcher(ex.getMessage());
                if (patternMatcher.find()) {
                    String filePath = hsacEnvironment.getFitNesseRootDir() + "/" + patternMatcher.group(1);
                    if(!attachments.contains(filePath)) {
                        attachments.add(filePath);
                        String msg;
                        String ext = FilenameUtils.getExtension(Paths.get(filePath).toString());
                        if (ext.equalsIgnoreCase(SCREENSHOT_EXT)) {
                            msg = "Page Screenshot";
                        } else if (ext.equalsIgnoreCase(PAGESOURCE_EXT)) {
                            msg = "Page Source";
                        } else {
                            msg = "Attachment";
                        }
                        ByteSource attachmentSource = Files.asByteSource(new File(filePath));
                        SaveLogRQ.File attachmentFile = new SaveLogRQ.File();
                        attachmentFile.setContent(attachmentSource);
                        attachmentFile.setName(msg);
                        sendAttachment(msg, "ERROR", context.getTestId(getFullTestName(result.getDescription())), attachmentFile);
                    }
                }
            }
        }
    }

    private Set<String> getTags(WikiPage page) {
        Set<String> tags = new HashSet<>();
        String tagInfo = page.getData().getProperties().get("Suites");
        if (null != tagInfo) {
            String[] tagArr = tagInfo.split(",");
            tags.addAll(Arrays.asList(tagArr));
        }
        return tags;
    }
}
