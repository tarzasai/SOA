package net.esorciccio.goa;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.provider.Settings;
import android.widget.RemoteViews;

public class OAWidgetLarge extends AppWidgetProvider {
	
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
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		//AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		@SuppressWarnings("deprecation")
		String alarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		views.setTextViewText(R.id.txt_left, OASession.timeString(session.getLeft()));
		views.setTextViewText(R.id.txt_today, OASession.dateString(System.currentTimeMillis(), "EEE d MMM"));
		views.setTextViewText(R.id.txt_alarm, alarm);
		//views.setTextViewText(R.id.txt_dname, session.getDayName());
		
		Bitmap bitmap = Bitmap.createBitmap(200, 80, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setSubpixelText(true);
		paint.setTypeface(session.getFontClock());
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);
		paint.setTextSize(60);
		paint.setTextAlign(Align.CENTER);
		//canvas.drawText(OASession.timeString(System.currentTimeMillis()), 100, 53, paint);
		canvas.drawText("23:48", 85, 45, paint);
		
		views.setImageViewBitmap(R.id.img_time, bitmap);
		
		awManager.updateAppWidget(awId, views);
	}
}