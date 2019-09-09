package nl.praegus.fitnesse.slim.fixtures.totp;

import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class TotpFixtureTest {
    String secretKey = "RBDM3GIZMXFXTY6O";
    private TotpFixture totpFixture = new TotpFixture();

    @Test
    public void whenASecretIsGivenByDefaultASixDigitTotpIsGenerated() {
        assertThat(totpFixture.getTotpForSecret(secretKey).length()).isEqualTo(6);
    }

    @Test
    public void numberOfDigitsCanBeSet() {
        totpFixture.setNumberOfDigits(8);
        assertThat(totpFixture.getTotpForSecret(secretKey).length()).isEqualTo(8);
    }

    @Test
    public void whenTheNumberOfDigitsSetIsLowerThanSixAnExceptionWillBeThrown() {
        Throwable thrown = catchThrowable(() -> totpFixture.setNumberOfDigits(5));
        assertThat(thrown).isInstanceOf(SlimFixtureException.class)
                .withFailMessage("TOTP length needs to be > 6 and < 8");
    }

    @Test
    public void whenTheNumberOfDigitsSetIsHigherThanEightAnExceptionWillBeThrown() {
        Throwable thrown = catchThrowable(() -> totpFixture.setNumberOfDigits(9));
        assertThat(thrown).isInstanceOf(SlimFixtureException.class)
                .withFailMessage("TOTP length needs to be > 6 and < 8");
    }

    @Test
    public void whenTotpIsGeneratedWithoutSecretAnExceptionWillBeThrown() {
        Throwable thrown = catchThrowable(() -> totpFixture.getTotp());
        assertThat(thrown).isInstanceOf(SlimFixtureException.class)
                .withFailMessage("No secret key was provided to generate OTP with");
    }

}
