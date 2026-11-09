package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugCleanShot --es preview describe|customise|on|factory` opens a screen of «UrTV النظيف» for a picture (the real screen is behind a signature
 * permission a shell does not hold): `describe` (off, the confirmation), `customise` (the sheet), `on` (it is on: the way back), `factory` (the second step: with no debugging on a CI emulator it shows its refusal).
 */
public class DebugCleanShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String state = getIntent().getStringExtra("preview");
        CleanMode.setOn(this, "on".equals(state));   // every picture sets the state it shows; «off» puts it back
        Intent i = "factory".equals(state) ? new Intent().setClassName(getPackageName(), "sa.urtv.home.setup.FactoryActivity") : new Intent(this, CleanActivity.class).putExtra("preview", state);
        startActivity(i);
        finish();
    }
}
