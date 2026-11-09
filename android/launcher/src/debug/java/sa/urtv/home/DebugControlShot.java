package sa.urtv.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Debug builds only: `am start -n sa.urtv.home/.DebugControlShot --es preview rest|focus|customise` opens the home with «مركز التحكم» open for a picture:
 * `rest` (the panel as it opens), `focus` (the focus on the sound row), `customise` («تخصيص»). The light look is the screensaver's «appearance» setting, as for the other pictures.
 */
public class DebugControlShot extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String state = getIntent().getStringExtra("preview");
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("open", "center");
        if ("focus".equals(state)) i.putExtra("ccview", "sound");
        else if ("customise".equals(state)) i.putExtra("ccview", "custom");
        startActivity(i);
        finish();
    }
}
