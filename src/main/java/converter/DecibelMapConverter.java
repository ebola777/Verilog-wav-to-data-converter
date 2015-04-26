package converter;

import model.DecibelMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecibelMapConverter {
    private static final String REGEX_VERILOG_NUMBER = "(?<sign>\\-?)"
            + "^(((\\d+)?'("
            + "([bB](?<binary>[01_]+))|"
            + "([oO](?<octal>[0-8_]+))|"
            + "([hH](?<hex>[0-9a-fA-F_]+))|"
            + "([dD](?<decimal>[\\d_]+)))"
            + ")|"
            + "(?<integer>\\d+))$";
    private static final Pattern patternVerilogNumber = Pattern.compile(REGEX_VERILOG_NUMBER);

    public static Map<String, Integer> getConvertedDecibelMap(final DecibelMap map) {
        final Map<String, Integer> convertedMap = new HashMap<String, Integer>();
        final Map<String, Object> db = map.getDb();

        for (final Map.Entry<String, Object> entry : db.entrySet()) {
            String dbLevel;
            int dbVerilogNumber;
            String verilogNumber;

            try {
                dbLevel = getDecibelLevel(entry.getKey());
                verilogNumber = getDecibelVerilogNumber(entry);
                dbVerilogNumber = getVerilogNumberToInteger(verilogNumber);

                convertedMap.put(dbLevel, dbVerilogNumber);
            } catch (final Exception e) {
                System.err.println(e.getMessage());
                System.err.printf("\t Decibel level: \"%s\"%n", entry.getKey().toString());
                System.err.printf("\t Verilog number: \"%s\"%n", entry.getValue().toString());
            }
        }

        return convertedMap;
    }

    private static String getDecibelVerilogNumber(final Map.Entry<String, Object> entry)
            throws Exception {
        String text;

        if (entry.getValue() instanceof List) {
            final List<?> texts = (List<?>) entry.getValue();
            Object firstText;

            if (texts.isEmpty()) {
                throw new Exception("Warning: Empty db value array.");
            }

            firstText = texts.get(0);

            if (!(firstText instanceof String)) {
                throw new Exception(String.format("Warning: Unexpected db value type %s.",
                        firstText.getClass().toString()));
            }

            text = (String) texts.get(0);
        } else if (entry.getValue() instanceof String) {
            text = (String) entry.getValue();
        } else {
            throw new Exception(String.format("Warning: Unexpected db value type %s.",
                    entry.getClass().toString()));
        }

        return text;
    }

    private static String getDecibelLevel(final String decibelLevel) {
        if (decibelLevel.equals("-Infinity")) {
            return "-Infinity";
        } else if (decibelLevel.equals("Others")) {
            return "Others";
        } else {
            return String.valueOf(Integer.parseInt(decibelLevel));
        }
    }

    private static int getVerilogNumberToInteger(final String verilogNumber) throws Exception {
        final Matcher matcher = patternVerilogNumber.matcher(verilogNumber);

        if (matcher.find()) {
            final String sign = matcher.group("sign");
            String binaryNumber = matcher.group("binary");
            String octalNumber = matcher.group("octal");
            String hexNumber = matcher.group("hex");
            String decimalNumber = matcher.group("decimal");
            final String integerNumber = matcher.group("integer");

            if (!sign.equals("") && !sign.equals("-")) {
                throw new Exception("Expected sign to be either empty or \"-\".");
            }

            if (binaryNumber != null) {
                binaryNumber = binaryNumber.replace("_", "");
            } else if (octalNumber != null) {
                octalNumber = octalNumber.replace("_", "");
            } else if (hexNumber != null) {
                hexNumber = hexNumber.replace("_", "");
            } else if (decimalNumber != null) {
                decimalNumber = decimalNumber.replace("_", "");
            }

            final Integer signToMultiply = sign.equals("-") ? -1 : 1;

            if (!binaryNumber.isEmpty()) {
                return signToMultiply * Integer.parseInt(binaryNumber, 2);
            } else if (!octalNumber.isEmpty()) {
                return signToMultiply * Integer.parseInt(binaryNumber, 8);
            } else if (!hexNumber.isEmpty()) {
                return signToMultiply * Integer.parseInt(hexNumber, 16);
            } else if (!decimalNumber.isEmpty()) {
                return signToMultiply * Integer.parseInt(decimalNumber);
            } else if (!integerNumber.isEmpty()) {
                return signToMultiply * Integer.parseInt(integerNumber);
            } else {
                throw new Exception(
                        "Warning: Expected either Verilog number to be non-empty.");
            }
        } else {
            throw new Exception("Warning: Cannot parse Verilog number.");
        }
    }
}
