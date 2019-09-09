package nl.praegus.fitnesse.slim.fixtures.totp;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TotpUtilsTest {
    @Test
    public void whenACodeHasLessDefaultNumberOfDigitsItWillBePaddedWithLeadingZeros() {
        int shortCode = 123;
        int expectNumberOfDigits = 6;
        assertThat(TotpUtils.padCodeWithZerosToNumberOfDigits(shortCode, expectNumberOfDigits))
                .isEqualTo("000123");
    }

    @Test
    public void whenACodeHasExactlyConfiguredNumberOfDigitsItWillNotBePaddedWithLeadingZeros() {
        int code = 12345678;
        int expectedNumberOfDigits = 8;
        assertThat(TotpUtils.padCodeWithZerosToNumberOfDigits(code, expectedNumberOfDigits))
                .isEqualTo("12345678");
    }
}
