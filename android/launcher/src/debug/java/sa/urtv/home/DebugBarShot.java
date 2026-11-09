package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugBarShot --es state expanded` opens the home with its top row held open (`collapsed`, the default: the row as it rests) for a picture.
 */
public class DebugBarShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if ("expanded".equals(getIntent().getStringExtra("state"))) i.putExtra("bar", "open");
        startActivity(i);
        finish();
    }
}
