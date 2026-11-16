package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** For tests/task_voice_test.py: TaskVoice.java on the cases in tests/task_voice_cases.tsv.
 *  Arg: the cases file. Prints one line per case: kind, index, text (tab-separated). */
public final class TaskVoiceRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);   // heard, tasks (";"; a leading "-" = done), then the expected result
            List<String> tasks = new ArrayList<>();
            List<Boolean> done = new ArrayList<>();
            for (String t : f[1].split(";")) {
                if (t.isEmpty()) continue;
                done.add(t.startsWith("-"));
                tasks.add(t.startsWith("-") ? t.substring(1) : t);
            }
            TaskVoice.Result r = f[0].startsWith("~") ? TaskVoice.spoken(f[0].substring(1), tasks, done, 60)   // "~": what «تكلّم» does (#48)
                    : TaskVoice.understand(f[0], tasks, done);
            out.append(r.kind).append('\t').append(r.index).append('\t').append(r.text).append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
