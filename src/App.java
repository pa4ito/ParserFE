import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ParserFE - ISO 8583 message parser for VISA, Mastercard and Way4 formats.
 *
 * Usage: java -jar ParserFE.jar {iso-format} {filePathName} [{file-type}]
 * Example: java -jar ParserFE.jar v samples/visa_0100.txt
 */
public class App {

    private static byte[] buf;

    public static void main(String[] args) {
        if (!isArgsValid(args)) {
            System.exit(1);
        }

        try {
            // Measuring the parsing time
            long timeStarted = System.nanoTime();

            ISOMessage msg = new ISOMessage(buf);

            switch (args[0]) {
                case "v":
                    msg.parseVisa();
                    break;

                case "m":
                    msg.parseMC();
                    break;

                case "w":
                    msg.parseWay4();
                    break;

                default:
                    printHelp();
                    System.exit(1);
            }

            long timeElapsed = System.nanoTime() - timeStarted;

            msg.printBrand();
            msg.printMTI();
            msg.printMessageLength();
            msg.printMessageRealLength();
            msg.printBitmap();
            msg.printFields();

            System.out.println();
            System.out.println("------------------------------------");
            System.out.printf("Parsing time: %d ns (%.3f ms)%n", timeElapsed, timeElapsed / 1_000_000.0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static boolean isArgsValid(String[] args) {
        if (args.length < 2) {
            printHelp();
            return false;
        }

        if (!args[0].matches("[vmw]")) {
            System.out.println("Unknown ISO format: " + args[0]);
            printHelp();
            return false;
        }

        Path path = Paths.get(args[1]);
        if (!Files.exists(path)) {
            System.out.println("File " + path.getFileName() + " not found!");
            printHelp();
            return false;
        }

        // Binary file if requested explicitly ("b") or if the extension is .bin,
        // otherwise a text file with hex digits (spaces and line breaks are ignored)
        boolean isBinary;
        if (args.length >= 3) {
            isBinary = args[2].equals("b");
        } else {
            isBinary = path.getFileName().toString().toLowerCase().endsWith(".bin");
        }

        try {
            if (isBinary) {
                buf = Files.readAllBytes(path);
            } else {
                buf = Util.hexDecode(
                        Files.readString(path, StandardCharsets.US_ASCII).replaceAll("\\s", ""));
            }
        } catch (IOException e) {
            System.out.println("Cannot read " + path + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    private static void printHelp() {
        System.out.println("------------------------------------");
        System.out.println("Usage: java -jar ParserFE.jar {iso-format} {filePathName} [{file-type}]");
        System.out.println("    {iso-format}   -> v(VISA) / m(Mastercard) / w(Way4)");
        System.out.println("    {filePathName} -> .txt (hex text file - default) / .bin (binary file)");
        System.out.println("    [{file-type}]  -> t(text file) / b(binary file)");
        System.out.println();
        System.out.println("Example: java -jar ParserFE.jar v samples/visa_0100.txt");
        System.out.println();
    }
}
