import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

public class RemoveBOM {
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java");

        try (Stream<Path> paths = Files.walk(srcDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(RemoveBOM::removeBOM);
        }

        System.out.println("BOM removal completed!");
    }

    private static void removeBOM(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length >= 3 &&
                bytes[0] == (byte)0xEF &&
                bytes[1] == (byte)0xBB &&
                bytes[2] == (byte)0xBF) {

                byte[] newBytes = new byte[bytes.length - 3];
                System.arraycopy(bytes, 3, newBytes, 0, newBytes.length);
                Files.write(file, newBytes);
                System.out.println("Fixed: " + file);
            }
        } catch (IOException e) {
            System.err.println("Error processing: " + file + " - " + e.getMessage());
        }
    }
}

