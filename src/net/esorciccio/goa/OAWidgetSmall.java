package net.esorciccio.goa;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class OAWidgetSmall extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds)
			updateAppWidget(context, appWidgetManager, appWidgetId);
	}
	
	@Override
	public void onEnabled(Context context) {
		OASession.getInstance(context).updateWidget();
	}
	
	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}
	
	static void updateAppWidget(Context context, AppWidgetManager awManager, int awId) {
		OASession session = OASession.getInstance(context);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		views.setTextViewText(R.id.txt_left, OASession.timeString(session.getLeft()));
		
		Intent launchActivity = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0);
		views.setOnClickPendingIntent(R.id.lil_widget, pendingIntent);
		
		awManager.updateAppWidget(awId, views);
	}
}