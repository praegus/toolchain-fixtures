package nl.praegus.fitnesse.slim.fixtures.util;

import org.mariuszgromada.math.mxparser.Expression;

import java.text.DecimalFormat;

public class Calculator {

    private Integer precision;

    public Calculator(int precision) {
        this.precision = precision;
    }

    public Calculator() {
    }

    public String calculate(String input) {
        StringBuilder df = new StringBuilder("#");
        Expression e = new Expression(input);
        double result = e.calculate();
        if (null != precision) {
            df.append(".");
            for(int i = 0; i < precision; i++) {
                df.append("#");
            }
        }
        return new DecimalFormat(df.toString()).format(result).replace(",", ".");
    }

}
