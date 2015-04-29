package main;

import converter.WaveConverter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;

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
        final ArgumentParser parser = createParser();
        final Namespace res = tryGetParsedArguments(args, parser);

        if (res == null) {
            System.exit(1);
            return;
        }

        printParsedArguments(res);

        if (!tryConvert(res)) {
            System.exit(1);
            return;
        }
    }

    private static ArgumentParser createParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("wav_to_data")
                .description("Convert WAV file to Verilog data file.");

        parser.addArgument("pathToOutput")
                .help("Path to output file.");
        parser.addArgument("pathToWav")
                .help("Path to wav file.");
        parser.addArgument("pathToMap")
                .help("Path to map file which contains decibel levels to Verilog numbers.");
        parser.addArgument("-f", "--format")
                .setDefault("binary")
                .choices("binary", "hex")
                .help("Output format. Default: \"binary\".");
        parser.addArgument("-w", "--bit_width")
                .setDefault(0)
                .type(Integer.class)
                .help("Output bit width. Fit to bit width of max value if omitted.");

        return parser;
    }

    private static Namespace tryGetParsedArguments(final String[] args,
            final ArgumentParser parser) {
        Namespace res;

        try {
            res = parser.parseArgs(args);
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
            return null;
        }

        return res;
    }

    private static void printParsedArguments(final Namespace res) {
        final String pathToOutput = res.getString("pathToOutput");
        final String pathToWav = res.getString("pathToWav");
        final String pathToMap = res.getString("pathToMap");
        final String format = res.getString("format");
        final String bitWidth = res.getString("bit_width");

        System.out.printf("pathToOutput: %s%n", pathToOutput);
        System.out.printf("pathToWav: %s%n", pathToWav);
        System.out.printf("pathToMap: %s%n", pathToMap);
        System.out.printf("--format: %s%n", format);
        System.out.printf("--bit_width: %s%n", bitWidth);
    }

    private static boolean tryConvert(final Namespace res) {
        try {
            final WaveConverter converter = createConverter(res);

            converter.convert();
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    private static WaveConverter createConverter(final Namespace res) throws Exception {
        final String pathToOutput = res.getString("pathToOutput");
        final String pathToWav = res.getString("pathToWav");
        final String pathToMap = res.getString("pathToMap");
        final String format = res.getString("format");
        final String bitWidth = res.getString("bit_width");

        final WaveConverter waveConverter =
                new WaveConverter(new File(pathToWav), new File(pathToOutput));

        waveConverter.setMapFile(new File(pathToMap));
        waveConverter.setFormat(format);
        waveConverter.setBitWidth(bitWidth);

        return waveConverter;
    }
}
