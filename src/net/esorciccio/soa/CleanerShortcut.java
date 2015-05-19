package net.esorciccio.soa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CleanerShortcut extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent shc = new Intent(getApplicationContext(), CleanerActivity.class);
		shc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shc.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		Intent res = new Intent();
		res.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shc);
		res.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_name));
		res.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
			Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_action_delete));
		setResult(RESULT_OK, res);
		
		finish();
	}
}