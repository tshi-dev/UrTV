package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugUpdateShot --es preview downloading` shows «تحديث تلفزيوني» in that state (available, downloading, waiting_owner, done)
 * without the network. An activity, not a receiver: a broadcast may not start an activity from the background on API 34, and a shell may not start UpdateActivity itself
 * (its signature permission), but this app may.
 */
public class DebugUpdateShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        startActivity(new Intent(this, UpdateActivity.class).putExtra("preview", getIntent().getStringExtra("preview")));
        finish();
    }
}
