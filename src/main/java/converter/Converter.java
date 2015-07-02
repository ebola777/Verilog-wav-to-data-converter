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
public class Converter {
    private static final int BUFFER_SIZE = 1024;

    private final File wavFile;
    private final File outputFile;
    private String format;
    private int bitWidth;
    private File mapFile;

    private Map<String, Integer> dbMap;

    public Converter(final File wavFile, final File outputFile) {
        this.wavFile = wavFile;
        this.outputFile = outputFile;

        this.dbMap = new HashMap<String, Integer>();
    }

    public void convert() throws Exception {
        final List<Integer> verilogNumbers = new ArrayList<Integer>();
        final WavFile wavFile = WavFile.openWavFile(this.wavFile);
        final int numChannels = wavFile.getNumChannels();
        final double[] buffer = new double[numChannels * BUFFER_SIZE];
        int framesRead;
        double minValue = 1;
        double maxValue = 0;

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

                minValue = Math.min(minValue, amplitudeNormalized);
                maxValue = Math.max(amplitudeNormalized, maxValue);
            }
        } while (framesRead != 0);

        System.out.printf("Min decibel: %f%n", this.getDecibelLevel(minValue));
        System.out.printf("Max decibel: %f%n", this.getDecibelLevel(maxValue));

        wavFile.close();

        this.saveOutput(verilogNumbers);
    }

    private void saveOutput(final List<Integer> verilogNumbers) throws Exception {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(this.outputFile));

        for (final int verilogNumber : verilogNumbers) {
            final String verilogNumberText = this.getIntegerToVerilogNumber(verilogNumber);

            writer.write(verilogNumberText);
            writer.newLine();
        }

        writer.close();

        // Output information
        System.out.printf("Verilog number count: %d%n", verilogNumbers.size());
        System.out.println("Finished.");
    }

    private void parseDecibelMap() {
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final DecibelMap originalDbMap =
                    mapper.readValue(this.getMapFile(), DecibelMap.class);

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
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < this.getBitWidth() - num.length(); ++i) {
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

    public void setFormat(final String format) throws Exception {
        if (format.equals("binary") || format.equals("hex")) {
            this.format = format;
        } else if (format.isEmpty()) {
            this.format = "binary";
        } else {
            throw new Exception(String.format("Unexpected format %s.", format));
        }
    }

    public int getBitWidth() {
        return this.bitWidth;
    }

    public void setBitWidth(final String bitWidth) throws NumberFormatException {
        this.bitWidth = Integer.parseInt(bitWidth);
    }

    public File getMapFile() {
        return this.mapFile;
    }

    public void setMapFile(final File mapFile) {
        this.mapFile = mapFile;

        this.parseDecibelMap();
    }
}
