package net.esorciccio.goa;

import java.text.DateFormat;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

public class OAAppWidget extends AppWidgetProvider {
	
	private OASession session;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		session = OASession.getInstance(context);
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds)
			updateAppWidget(context, session, appWidgetManager, appWidgetId);
		// When we click the widget, we want to open our main activity.
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_1_1);
		Intent launchActivity = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0);
		rv.setOnClickPendingIntent(R.id.lil_widget, pendingIntent);
		ComponentName thisWidget = new ComponentName(context, OAAppWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, rv);
	}
	
	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}
	
	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}
	
	static void updateAppWidget(Context context, OASession session, AppWidgetManager awManager, int awId) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_1_1);
		
		views.setTextViewText(R.id.txt_arrival, session.getArrival() <= 0 ? "n/a" : DateUtils.formatSameDayTime(
			session.getArrival(), System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT));
		
		views.setTextViewText(R.id.txt_leaving, session.getArrival() <= 0 ? "n/a" : DateUtils.formatSameDayTime(
			session.getLeaving(), System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT));
		
		views.setTextViewText(R.id.txt_left, session.getLeft() <= 0 ? "n/a" : DateUtils.formatSameDayTime(
			session.getLeft(), System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT));
		
		awManager.updateAppWidget(awId, views);
	}
}