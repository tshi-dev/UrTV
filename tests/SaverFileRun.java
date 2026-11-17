package sa.urtv.screensaver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** For tests/saver_file_test.py: prints SaverFile.json(SaverFile.fromGet(the file's text)), or «null». */
public final class SaverFileRun {
    public static void main(String[] a) throws Exception {
        String text = new String(Files.readAllBytes(Paths.get(a[0])), StandardCharsets.UTF_8);
        java.util.Map<String, Object> m = SaverFile.fromGet(text);
        System.out.print(m == null ? "null" : SaverFile.json(m));
    }
}
