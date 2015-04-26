package converter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.DecibelMap;
import sound.WavFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert wav file to db level integer file.
 *
 * Formula of calculating db level: Decibel[f_] := 20*Log[10, f];
 * f ranges from 0 (Min) to 1 (Max).
 */
public class WaveConverter {
    private static final int BUFFER_SIZE = 1024;

    private final String pathToWav;
    private final String pathToOutput;
    private String format;
    private String bitWidth;
    private String pathToMap;

    private Map<String, Integer> dbMap;

    public WaveConverter(final String pathToWav, final String pathToOutput) {
        this.pathToWav = pathToWav;
        this.pathToOutput = pathToOutput;

        this.dbMap = new HashMap<String, Integer>();
    }

    public void convert() {
        final List<Integer> verilogNumbers = new ArrayList<Integer>();

        this.parseDecibelMap();

        try {
            final WavFile wavFile = WavFile.openWavFile(new File(this.pathToWav));
            final int numChannels = wavFile.getNumChannels();
            final double[] buffer = new double[numChannels * BUFFER_SIZE];
            int framesRead;
            double maxValue = 0;
            double minValue = 1;

            // Display information about the wav file
            wavFile.display();

            do {
                framesRead = wavFile.readFrames(buffer, BUFFER_SIZE);

                for (int s = 0; s < framesRead * numChannels; ++s) {
                    int verilogNumber;
                    double dbLevel;
                    final double amplitude = buffer[s];
                    final double amplitudeNormalized = (amplitude + 1) / 2;

                    dbLevel = this.getDecibelLevel(amplitudeNormalized);
                    verilogNumber = this.getVerilogNumber(dbLevel);
                    verilogNumbers.add(verilogNumber);

                    if (amplitudeNormalized > maxValue) {
                        maxValue = amplitudeNormalized;
                    } else if (amplitudeNormalized < minValue) {
                        minValue = amplitudeNormalized;
                    }
                }
            } while (framesRead != 0);

            System.out.printf("Max decibel: %f%n", this.getDecibelLevel(maxValue));
            System.out.printf("Min decibel: %f%n", this.getDecibelLevel(minValue));

            wavFile.close();

            this.saveOutput(verilogNumbers);
        } catch (final Exception e) {
            System.err.println(e);
        }
    }

    private void saveOutput(final List<Integer> verilogNumbers) throws Exception {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(this.pathToOutput));

        for (final int verilogNumber : verilogNumbers) {
            final String verilogNumberText = this.getIntegerToVerilogNumber(verilogNumber);

            writer.write(verilogNumberText);
            writer.newLine();
        }

        writer.close();

        // Output information
        System.out.printf("Verilog number count: %d%n", verilogNumbers.size());
        System.out.println("Finish.");
    }

    private void parseDecibelMap() {
        if (this.getPathToMap().isEmpty()) {
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();

        try {
            final DecibelMap originalDbMap =
                    mapper.readValue(new File(this.getPathToMap()), DecibelMap.class);

            this.dbMap = DecibelMapConverter.getConvertedDecibelMap(originalDbMap);
        } catch (final JsonParseException e) {
            System.err.println("Error: Failed to parse Json.");
        } catch (final JsonMappingException e) {
            System.err.println("Error: Failed to map Json.");
        } catch (final IOException e) {
            System.err.println("Error: Failed to open Json file.");
        }
    }

    private String getIntegerToVerilogNumber(final int num) throws Exception {
        if (this.format.equals("binary")) {
            return this.getPaddedNumber(Integer.toBinaryString(num));
        } else if (this.format.equals("hex")) {
            return this.getPaddedNumber(Integer.toHexString(num));
        } else {
            throw new Exception(String.format("Unexpected format %s.", this.format));
        }
    }

    private int getVerilogNumber(final double dbLevel) throws Exception {
        String dbLevelText;

        if (Double.isInfinite(dbLevel)) {
            dbLevelText = "-Infinity";
        } else {
            dbLevelText = String.valueOf((int) Math.round(dbLevel));
        }

        if (this.dbMap.containsKey(dbLevelText)) {
            return this.dbMap.get(dbLevelText);
        } else if (this.dbMap.containsKey("Others")) {
            return this.dbMap.get("Others");
        } else {
            throw new Exception(
                    String.format("No map for decibel level %s", dbLevelText));
        }
    }

    private String getPaddedNumber(final String num) {
        if (this.bitWidth == null) {
            return num;
        }

        final StringBuilder sb = new StringBuilder();
        final int bitWidthNum = Integer.parseInt(this.bitWidth);

        for (int i = 0; i < bitWidthNum - num.length(); ++i) {
            sb.append("0");
        }

        sb.append(num);

        return sb.toString();
    }

    private double getDecibelLevel(final double value) {
        return 20 * Math.log10(value);
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getBitWidth() {
        return this.bitWidth;
    }

    public void setBitWidth(final String bitWidth) {
        this.bitWidth = bitWidth;
    }

    public String getPathToMap() {
        return this.pathToMap;
    }

    public void setPathToMap(final String pathToMap) {
        this.pathToMap = pathToMap;
    }
}
