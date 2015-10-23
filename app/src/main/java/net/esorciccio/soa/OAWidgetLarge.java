package net.esorciccio.soa;

import net.esorciccio.soa.serv.OASession;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;

public class OAWidgetLarge extends AppWidgetProvider {

	private void updateGUI(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		OASession session = OASession.getInstance(context);

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);

		views.setTextViewText(R.id.txt_enter, OASession.timeString(session.getArrival()));

		long ttogo = session.getLeaving();
		long tleft = session.getLeft();
		if (tleft > 0 && tleft < ttogo) {
			views.setTextColor(R.id.txt_leaving, Color.BLUE);
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(tleft));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_static_left, 0, 0, 0);
		} else {
			views.setTextColor(R.id.txt_leaving, Color.argb(87, 46, 46, 46));
			views.setTextViewText(R.id.txt_leaving, OASession.timeString(ttogo));
			views.setTextViewCompoundDrawables(R.id.txt_leaving, R.drawable.ic_static_leave, 0, 0, 0);
		}

		int ci;
		if (OASession.isOnWIFI)
			ci = R.drawable.ic_static_wifi;
		else if (!OASession.isOn3G)
			ci = R.drawable.ic_static_noconn;
		else if (OASession.isRoaming)
			ci = R.drawable.ic_static_roaming;
		else
			ci = R.drawable.ic_static_cell;

		views.setTextViewText(R.id.txt_network, OASession.network);
		views.setTextViewCompoundDrawables(R.id.txt_network, ci, 0, 0, 0);

		views.setOnClickPendingIntent(R.id.box_left, PendingIntent.getActivity(context, 0,
			new Intent(context, SettingsActivity.class), 0));

		views.setOnClickPendingIntent(R.id.txt_voldn, PendingIntent.getBroadcast(context, 0,
			new Intent(OASession.WR.VOLUME_DOWN), 0));

		views.setOnClickPendingIntent(R.id.txt_volup, PendingIntent.getBroadcast(context, 0,
			new Intent(OASession.WR.VOLUME_UP), 0));

		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds)
			updateGUI(context, appWidgetManager, appWidgetId);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		updateGUI(context, appWidgetManager, appWidgetId);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		//
	}

	@Override
	public void onEnabled(Context context) {
		//
	}

	@Override
	public void onDisabled(Context context) {
		//
	}
}