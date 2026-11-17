package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** For tests/saver_file_test.py: prints SaverFile.json(SaverFile.fromGet(the file's text)), or «null». */
public final class SaverFileRun {
    public static void main(String[] a) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get(a[0])), StandardCharsets.UTF_8);
        if (a.length > 1 && a[1].equals("extras")) {   // a schema-1 file → its SET extras, one «name=value» per line
            try {
                @SuppressWarnings("unchecked") java.util.Map<String, Object> f = (java.util.Map<String, Object>) SaverFile.parseJson(text);
                for (java.util.Map.Entry<String, String> e : SaverFile.extras(f).entrySet()) System.out.print(e.getKey() + "=" + e.getValue() + "\n");
            } catch (IllegalArgumentException e) {
                System.out.print("refused: " + e.getMessage());
            }
            return;
        }
        java.util.Map<String, Object> m = SaverFile.fromGet(text);
        System.out.print(m == null ? "null" : SaverFile.json(m));
    }
}
