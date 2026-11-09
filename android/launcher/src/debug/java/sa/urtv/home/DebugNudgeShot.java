package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugNudgeShot` forgets any «لاحقاً» and opens the home, so «خلّ تلفزيونك يفتح على UrTV» shows (an emulator keeps its own
 * home, so we are not the Home there) for a picture.
 */
public class DebugNudgeShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getSharedPreferences("home_nudge", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
