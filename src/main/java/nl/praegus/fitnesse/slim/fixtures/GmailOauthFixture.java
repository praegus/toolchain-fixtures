package nl.praegus.fitnesse.slim.fixtures;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.util.FileUtil;
import org.apache.commons.text.StringEscapeUtils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailOauthFixture extends SlimFixture {
    private String APPLICATION_NAME;
    private FileDataStoreFactory DATA_STORE_FACTORY;
    private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private HttpTransport HTTP_TRANSPORT;
    private List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private String filterQuery = "";
    private Gmail service;
    private String user = "me"; //Default to special user "me" - pointing to the logged in user
    private String latestMessageBody = "";
    private List<String> latestMessageAttachments = new ArrayList<>();
    private String latestMessageId;
    private String clientSecretPostfix; //use postfixes if you need more than 1 gmail account in one test project
    private String bodyMimeType = "text/plain";
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=!]*)");

    /**
     * Create a gmail connection for the given App
     *
     * @param appName The name of the application, as configured in the GmailAPI at Google
     */
    public GmailOauthFixture(String appName) {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            APPLICATION_NAME = appName;
            File DATA_STORE_DIR = new File(this.filesDir, String.format("gmailOauthFixture/%s/.credentials/gmail-credentials", APPLICATION_NAME));
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
            service = getGmailService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a postfix for the client secret file. Useful when multiple GMail adresses are used in one project.
     *
     * @param clientSecretPostfix The postfix to use.
     */
    public void setClientSecretPostfix(String clientSecretPostfix) {
        this.clientSecretPostfix = clientSecretPostfix;
    }

    /**
     * Set the filter query to filter the inbox.
     *
     * @param filterQuery The query to use. Documented at: https://support.google.com/mail/answer/7190
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    /**
     * Specify the message format of the message you expect.
     *
     * @param format The format: either plain (default) or html.
     */
    public void setMessageFormat(String format) { //Can be plain or html
        bodyMimeType = "text/" + format;
    }

    private void setLatestMessageBody(String base64EncodedMessageBody) {
        latestMessageBody = StringUtils.newStringUtf8(Base64.decodeBase64(base64EncodedMessageBody));
    }

    /**
     * Get the body of the latest email message matching the specified search query. (Or newest in the inbox if no search query is specified)
     *
     * @return The body text (either plain text or HTML) as a String.
     */
    public String latestMessageBody() {
        if (bodyMimeType.equalsIgnoreCase("text/html")) {
            return "<pre>" + StringEscapeUtils.escapeHtml4(latestMessageBody) + "</pre>";
        }
        return latestMessageBody;
    }

    /**
     * Save the body of the latest email message matching the specified search query. (Or newest in the inbox if no search query is specified) to a file
     *
     * @param fileName The filename to save the body to.
     * @return a link to the created file
     */
    public String saveLatestEmailBody(String fileName) {
        String fullName = String.format("%s/emails/%s.html", this.filesDir, fileName);
        File f = new File(fullName);
        File parentFile = f.getParentFile();
        parentFile.mkdirs();
        f = FileUtil.writeFile(fullName, latestMessageBody);
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", this.getEnvironment().getWikiUrl(f.getAbsolutePath()), f.getName());
    }

    /**
     * Boolean value that shows if the latest (filtered) message contains the specified needle (String)
     *
     * @param needle The text to find
     * @return true if needle was found, false otherwise
     */
    public boolean latestMessageBodyContains(String needle) {
        return latestMessageBody.contains(needle);
    }

    /**
     * Get the attachment names from the latest (filtered) message
     *
     * @return a list of filenames attached to the message
     */
    public List<String> latestMessageAttachments() {
        return latestMessageAttachments;
    }

    /**
     * Get a link from the latest (filtered) message that contains the specified needle (String)
     *
     * @param needle The text to search for
     * @return the url if a matching url was found, otherwise return an empty String
     */
    public String getLinkContaining(String needle) {
        Matcher urlMatcher = URL_PATTERN.matcher(latestMessageBody);
        while (urlMatcher.find()) {
            if (urlMatcher.group(0).contains(needle)) {
                return urlMatcher.group(0);
            }
        }
        return "";
    }

    /**
     * Move the latest (filterd) message to the trash folder
     *
     * @return true if the message was moved. False if no message was found.
     */
    public boolean trashCurrentMessage() {
        try {
            getAllMessages().trash(user, latestMessageId).execute();
            return true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Permanently delete the latest (filtered) message
     *
     * @return true if the message was deleted. False if no message was found.
     */
    public boolean deleteCurrentMessage() {
        try {
            getAllMessages().delete(user, latestMessageId).execute();
            return true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all messages matching a given query.  E.g. query "in:inbox" empties inbox. If no messages are found
     * no delete is executed.
     *
     * @param query gmail query as described on https://support.google.com/mail/answer/7190?hl=en
     * @throws IOException if no messages were found
     */
    public void deleteAllMessagesMatchingQuery(String query) throws IOException {
        List<Message> messages = filteredInbox();
        setFilterQuery(query);
        if (messages != null) {
            batchDeleteMessages(getMessageIds(messages));
        }
    }

    /**
     * Send an email using data from the message map.
     *
     * @param emailData A map containing to, from, subject, body, attachment (optional) as strings.
     *                  Attachment can be a wiki or absolute url pointing to a file to attach.
     * @return true if sending completes successfully
     */
    public boolean sendEmail(Map<String, String> emailData) {
        MimeMessage msg;
        try {
            if (emailData.containsKey("attachment") && emailData.get("attachment").length() > 0) {
                msg = createMessageWithAttachment(emailData.get("to"),
                        emailData.get("from"),
                        emailData.get("subject"),
                        emailData.get("body"),
                        new File(getFilePathFromWikiUrl(emailData.get("attachment"))));
            } else {
                msg = createMessageWithText(emailData.get("to"),
                        emailData.get("from"),
                        emailData.get("subject"),
                        emailData.get("body"));
            }

            Message message = createMessageWithEmail(msg);
            message = service.users().messages().send(user, message).execute();
            System.out.println("Sent message with id: " + message.getId());
            return true;
        } catch (javax.mail.MessagingException | IOException e) {
            throw new SlimFixtureException(false, "Error sending email", e);
        }


    }

    private MimeMessage createMessageWithText(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private MimeMessage createMessageWithAttachment(String to, String from, String subject, String bodyText, File file) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(file);

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(file.getName());

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent)
            throws javax.mail.MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private void processPart(MessagePart part, StringBuilder sb, List<String> attachments) {
        if (part.getParts() != null) {
            for (MessagePart prt : part.getParts()) {
                processPart(prt, sb, attachments);
            }
        } else {
            if (part.getMimeType().equalsIgnoreCase(bodyMimeType)) {
                sb.append(part.getBody().getData());
            } else if (part.getMimeType().toLowerCase().startsWith("application")) {
                attachments.add(part.getFilename());
            }
        }
    }

    /**
     * Poll the inbox until a message (matching the filter if specified) arrives.
     *
     * @return true if a message was found within the maximum number of retries, false otherwise
     */
    public boolean pollUntilMessageArrives() {
        return repeatUntil(inboxRetrievedCompletion());
    }

    protected FunctionalCompletion inboxRetrievedCompletion() {
        return new FunctionalCompletion(() -> ((null != filteredInbox() && filteredInbox().size() != 0)));
    }

    private void getLatestMessageInfo() {
        try {
            StringBuilder sb = new StringBuilder();
            List<String> attachments = new ArrayList<>();
            Message message = getAllMessages().get(user, latestMessageId).setFormat("full").execute();

            if (message.getPayload().getParts() != null) {
                for (MessagePart part : message.getPayload().getParts()) {
                    processPart(part, sb, attachments);
                }
                setLatestMessageBody(sb.toString());
            } else {
                setLatestMessageBody(message.getPayload().getBody().getData());
                latestMessageAttachments = attachments;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Credential authorize() throws IOException {
        // Load client secrets. - Make sure to put them in your resources folder!
        String clientSecret = "/gmail_client_secret";
        if (null != clientSecretPostfix) {
            clientSecret += "_" + clientSecretPostfix;
        }
        InputStream in =
                GmailOauthFixture.class.getResourceAsStream(clientSecret + ".json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();

        return new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
    }

    private Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private List<Message> filteredInbox() {
        try {
            ListMessagesResponse msgResponse = getAllMessages().list(user).setQ(filterQuery).execute();
            List<Message> messages = msgResponse.getMessages();
            if (null != messages && messages.size() > 0) {
                latestMessageId = messages.get(0).getId();
                getLatestMessageInfo();
            }
            return messages;
        } catch (IOException e) {
            System.err.println("Exception fetching e-mail: " + e.getMessage());
            return null;
        }
    }

    private void batchDeleteMessages(List<String> messageIds) throws IOException {
        getAllMessages().batchDelete(user, new BatchDeleteMessagesRequest().setIds(messageIds)).execute();
    }

    private Messages getAllMessages() {
        return service.users().messages();
    }

    private List<String> getMessageIds(List<Message> messages) {
        List<String> filteredInboxMessageIds = new ArrayList<>();
        messages.forEach(message -> filteredInboxMessageIds.add(message.getId()));
        return filteredInboxMessageIds;
    }
}
