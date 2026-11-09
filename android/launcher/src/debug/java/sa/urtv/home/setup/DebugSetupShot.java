package sa.urtv.home.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug builds only (CI's pictures): `am start -n sa.urtv.home/.setup.DebugSetupShot --es preview progress` shows «إعداد تلفزيوني» in a state (start, plan, progress, restart, done)
 * without a TV to set up: the runner's own fields are set to what that state looks like (nothing runs), then SetupActivity is started.
 */
public class DebugSetupShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try { fake(getIntent().getStringExtra("preview")); } catch (Exception e) { android.util.Log.w("DebugSetupShot", e); }
        startActivity(new Intent(this, SetupActivity.class));
        finish();
    }

    private static void set(SetupRunner r, String name, Object v) throws Exception {
        Field f = SetupRunner.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(r, v);
    }

    private static void fake(String state) throws Exception {
        SetupRunner r = SetupRunner.get();
        List<SetupRunner.Step> steps = new ArrayList<>();
        for (String id : SetupRunner.ids(SetupRunner.plan(SetupRunner.Kind.FULL))) steps.add(new SetupRunner.Step(id));
        SetupRunner.Preview p = new SetupRunner.Preview();
        p.safe = 12; p.saverInstalled = false; p.tvbro = false; p.smarttube = false; p.restarts = true; p.homeOurs = false;
        set(r, "steps", new ArrayList<SetupRunner.Step>());
        set(r, "kind", null);
        set(r, "running", false);
        set(r, "end", null);
        set(r, "preview", p);
        set(r, "awaitingConfirm", false);
        if ("start".equals(state)) return;
        set(r, "kind", SetupRunner.Kind.FULL);
        if ("plan".equals(state)) { set(r, "running", true); set(r, "awaitingConfirm", true); return; }
        int upTo = "progress".equals(state) ? 7 : "restart".equals(state) ? 3 : steps.size();
        for (int i = 0; i < steps.size(); i++) steps.get(i).state = i < upTo ? SetupRunner.State.DONE : SetupRunner.State.PENDING;
        if ("progress".equals(state) || "restart".equals(state)) {
            steps.get(upTo).state = SetupRunner.State.RUNNING;
            set(r, "running", true);
            Field ring = SetupRunner.class.getDeclaredField("ring");
            ring.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.ArrayDeque<SetupRunner.Line> q = (java.util.ArrayDeque<SetupRunner.Line>) ring.get(r);
            q.clear();
            q.add(new SetupRunner.Line(1, "progress".equals(state) ? "نثبّت شاشة التوقف…" : "نقيس الذاكرة قبل الإعداد…"));
            set(r, "seq", 1L);
        } else {
            set(r, "end", new SetupRunner.End("اكتمل الإعداد.", false));
        }
        set(r, "steps", steps);
    }
}
