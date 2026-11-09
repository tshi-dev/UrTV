package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugSnapShot --es preview hold` opens the home with its stand-in picture (the last picture of the home, or the
 * skeleton when there is none) held in front for a picture; any other state is the normal start, where the picture fades out when the live views have painted.
 */
public class DebugSnapShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if ("hold".equals(getIntent().getStringExtra("preview"))) i.putExtra("snap", "hold");
        startActivity(i);
        finish();
    }
}
