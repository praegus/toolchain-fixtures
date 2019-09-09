package nl.praegus.fitnesse.slim.fixtures.totp;

public class TotpUtils {

    public static String padCodeWithZerosToNumberOfDigits(int code, int numberOfDigits) {
        String formatSpec = "%0" + numberOfDigits + "d";
        return String.format(formatSpec, code);
    }
}
