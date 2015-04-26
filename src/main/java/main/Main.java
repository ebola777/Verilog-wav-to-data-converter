package main;

import converter.WaveConverter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Example:
 *
 * wav_to_data
 * --format binary
 * --bit_width 6
 * --map test/db_map/UDA1330ATS.json
 * test/sound/c4_click.wav
 * test/output/c4_click.bin
 */
public class Main {
    public static void main(final String[] args) {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("prog")
                .description("Convert .wav to Verilog data file.");

        parser.addArgument("pathToWav")
                .help("Path to wav file.");
        parser.addArgument("pathToOutput")
                .help("Path to output file.");
        parser.addArgument("-f", "--format")
                .setDefault("binary")
                .help("Output format. \"binary\" or \"hex\".");
        parser.addArgument("-w", "--bit_width")
                .help("Output bit width. Fit to bit width of max value if omitted.");
        parser.addArgument("-m", "--map")
                .help("Map file which contains decibel levels to Verilog numbers.");

        Namespace res;

        try {
            res = parser.parseArgs(args);

            if (!checkArguments(res)) {
                return;
            }
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
            return;
        }

        final WaveConverter converter = createConverter(res);

        converter.convert();
    }

    private static WaveConverter createConverter(final Namespace res) {
        final String pathToWav = res.getString("pathToWav");
        final String pathToOutput = res.getString("pathToOutput");
        final String format = res.getString("format");
        final String bitWidth = res.getString("bit_width");
        final String pathToMap = res.getString("map");

        System.out.printf("pathToWav: %s%n", pathToWav);
        System.out.printf("pathToOutput: %s%n", pathToOutput);
        System.out.printf("--format: %s%n", format);
        System.out.printf("--bit-width: %s%n", bitWidth);
        System.out.printf("--pathToMap: %s%n", pathToMap);

        final WaveConverter waveConverter =
                new WaveConverter(pathToWav, pathToOutput);

        waveConverter.setFormat(format);
        waveConverter.setBitWidth(bitWidth);
        waveConverter.setPathToMap(pathToMap);

        return waveConverter;
    }

    private static boolean checkArguments(final Namespace res) {
        final String format = res.get("format");

        if (!format.equals("binary") && !format.equals("hex")) {
            System.err.println("Error: Unexpected --format value.");
            return false;
        }

        return true;
    }
}
