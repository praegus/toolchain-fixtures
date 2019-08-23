package nl.praegus.fitnesse.slim.fixtures;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.jcraft.jsch.*;
import nl.hsac.fitnesse.fixture.slim.FileFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

public class SftpFixture extends FileFixture {
    private JSch jsch = new JSch();
    private Session session;
    private ChannelSftp sftpChannel;

    private String username;
    private String password;
    private String host;
    private int port = 22;

    protected SftpFixture(JSch jsch, Session session, ChannelSftp sftpChannel) {
        this.jsch = jsch;
        this.session = session;
        this.sftpChannel = sftpChannel;
    }

    public SftpFixture() {
    }

    /**
     * Set the username
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set the password
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set the host to connect to
     *
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Set the port number
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Set a private key identity
     *
     * @param privateKey The key as String
     * @throws JSchException
     */
    public void setPrivateKey(String privateKey) throws JSchException {
        jsch.addIdentity(privateKey);
    }

    /**
     * Set a private key identity, using a file and passphrase
     * Usage: | set private key file | [privateKeyFile] | with passphrase | [passphrase] |
     *
     * @param privateKey The key file path
     * @param passphrase The passphrase to use
     * @throws JSchException
     */
    public void setPrivateKeyFileWithPassphrase(String privateKey, String passphrase) throws JSchException {
        jsch.addIdentity(privateKey, passphrase);
    }

    /**
     * Download a remote file
     *
     * @param remoteFile The file to download
     * @param localFile  The filename to write the downloaded file to
     * @return A link to the downloaded file
     */
    public String downloadFileTo(String remoteFile, String localFile) {
        try {
            connect();
            openChannel();
            return createContaining(localFile, FileUtil.streamToString(sftpChannel.get(remoteFile), remoteFile));
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to download: " + remoteFile, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Upload a local file to a remote location
     *
     * @param localFile  The file to upload
     * @param remoteFile The remote file to write to
     * @return true if the file was successfully copied. False otherwise.
     */
    public boolean uploadFileTo(String localFile, String remoteFile) {
        try {
            connect();
            openChannel();
            sftpChannel.put(getFullName(localFile), remoteFile);
            return true;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to upload " + localFile + " to " + remoteFile, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Delete a remote file
     *
     * @param remoteFile
     * @return true if the file was successfully deleted. False otherwise.
     */
    public boolean deleteFile(String remoteFile) {
        try {
            connect();
            openChannel();
            sftpChannel.rm(remoteFile);
            return true;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to delete: " + remoteFile, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Delete a renote directory
     *
     * @param remoteDir
     * @return true if the directory was successfully deleted. False otherwise.
     */
    public boolean deleteDirectory(String remoteDir) {
        try {
            connect();
            openChannel();
            sftpChannel.rmdir(remoteDir);
            return true;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to delete directory: " + remoteDir, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Create a renote directory
     *
     * @param remoteDir
     * @return true if the directory was successfully created. False otherwise.
     */
    public boolean createDirectory(String remoteDir) {
        try {
            connect();
            openChannel();
            sftpChannel.mkdir(remoteDir);
            return true;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to create directory: " + remoteDir, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * List the files in a renote directory
     *
     * @param remoteDir
     * @return a list of file names in the specified directory
     */
    public List<String> listFiles(String remoteDir) {
        try {
            List<String> result = new ArrayList<>();
            connect();
            openChannel();
            Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(remoteDir);
            for (ChannelSftp.LsEntry entry : list) {
                result.add(entry.getFilename());
            }
            return result;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to list contents of directory: " + remoteDir, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Get the text content of a renote file as a String
     *
     * @param remoteFile
     * @return The file's contents
     */
    public String textInRemoteFile(String remoteFile) {
        try {
            connect();
            openChannel();
            InputStream stream = sftpChannel.get(remoteFile);
            ByteSource streamBytes = new ByteSource() {
                @Override
                public InputStream openStream() {
                    return stream;
                }
            };
            return streamBytes.asCharSource(Charsets.UTF_8).read();
        } catch (JSchException | SftpException | IOException e) {
            throw new SlimFixtureException("Failed to list contents of file: " + remoteFile, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    /**
     * Get the text content of a renote file as a formatted HTML String
     *
     * @param remoteFile
     * @return The file's contents as formatted HTML
     */
    public String contentOfRemoteFile(String remoteFile) {
        String content = this.textInRemoteFile(remoteFile);
        return this.getEnvironment().getHtml(content);
    }

    /**
     * Poll until a file matching a given pattern exists in a given directory
     * Use 'repeatAtMostTimes' and 'setRepeatInterval' to configure the repeat behaviour.
     * Usage: | poll until file matching | [filePattern] | exists in | [directory] |
     *
     * @param filePattern
     * @param directory
     * @return The filename if a matching file was found. Null otherwise
     */
    public String pollUntilFileMatchingExistsIn(String filePattern, String directory) {
        if (repeatUntil(fileExistsInDirectoryCompletion(1, filePattern, directory))) {
            return findPatternInDirectory(filePattern, directory).first();
        } else {
            return null;
        }
    }

    /**
     * Poll until at least n files matching a given pattern exist in a given directory
     * Use 'repeatAtMostTimes' and 'setRepeatInterval' to configure the repeat behaviour.
     * Usage: | poll until | [number] | files matching | [filePattern] | exist in | [directory] |
     *
     * @param number      The number of files to poll for
     * @param filePattern
     * @param directory
     * @return A list of filenames if at least [number] of matching files are found. Null otherwise
     */
    public List<String> pollUntilFilesMatchingExistIn(int number, String filePattern, String directory) {
        if (repeatUntil(fileExistsInDirectoryCompletion(number, filePattern, directory))) {
            return new ArrayList<>(findPatternInDirectory(filePattern, directory));
        } else {
            return null;
        }
    }

    protected FunctionalCompletion fileExistsInDirectoryCompletion(int number, String filePattern, String directory) {
        return new FunctionalCompletion(() -> atLeastfilesExistInDirectory(number, filePattern, directory));
    }

    protected boolean atLeastfilesExistInDirectory(int number, String filePattern, String directory) {
        return findPatternInDirectory(filePattern, directory).size() >= number;
    }

    protected TreeSet<String> findPatternInDirectory(String filePattern, String directory) {
        List<String> ls = listFiles(directory);
        TreeSet<String> result = new TreeSet<>();
        for (String file : ls) {
            if (file.matches(filePattern)) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Set the permissions of a remote file to a numerical (chmod) permission
     *
     * @param remoteFile
     * @param chmodPermissions
     * @return true if chmod completes with no exception, false otherwise
     */
    public boolean setPermissionsOfTo(String remoteFile, String chmodPermissions) {
        try {
            connect();
            openChannel();
            int decimalPermissions = Integer.parseInt(chmodPermissions, 8);
            sftpChannel.chmod(decimalPermissions, remoteFile);
            return true;
        } catch (JSchException | SftpException e) {
            throw new SlimFixtureException("Failed to set permissions of : " + remoteFile + " to " + chmodPermissions, e);
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }
    }

    private void openChannel() throws JSchException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftpChannel = (ChannelSftp) channel;
    }

    private void connect() throws JSchException {
        if (null == session || !session.isConnected()) {
            session = jsch.getSession(username, host, port);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no"); //Convenient, but not very secure. Use only for testing
            session.setConfig(config);

            if (null != password) {
                session.setPassword(password);
            }
            session.connect();
        }
    }
}
