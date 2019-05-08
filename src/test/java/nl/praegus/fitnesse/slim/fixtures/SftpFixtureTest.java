package nl.praegus.fitnesse.slim.fixtures;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SftpFixtureTest {

    @Mock
    private JSch jsch;

    @Mock
    private Session session;

    @Mock
    private ChannelSftp sftpChannel;

    @InjectMocks
    private SftpFixture sftpFixture;

    @Test
    public void when_a_remote_file_is_downloaded_it_is_downloaded() throws JSchException, SftpException {
        sftpFixture.setUsername("username");
        sftpFixture.setHost("password");
        sftpFixture.setPort(80);

        when(jsch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
        when(session.openChannel("sftp")).thenReturn(sftpChannel);
        when(sftpChannel.get("file.html")).thenReturn(new ByteArrayInputStream("file.html".getBytes(Charset.forName("UTF-8"))));

        String result = sftpFixture.downloadFileTo("file.html", "file.html");

        assertThat(result).isEqualTo("<a href=\"files/fileFixture/file.html\" target=\"_blank\">file.html</a>");

        verify(jsch, times(1)).getSession("username", "password", 80);
        verify(session, times(1)).openChannel("sftp");
    }
}