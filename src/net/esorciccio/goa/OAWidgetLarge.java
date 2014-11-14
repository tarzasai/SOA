package net.esorciccio.goa;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
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
	
	public Bitmap buildUpdate(String time) {
		Bitmap myBitmap = Bitmap.createBitmap(160, 84, Bitmap.Config.ARGB_4444);
		Canvas myCanvas = new Canvas(myBitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setSubpixelText(true);
		paint.setTypeface(clock);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);
		paint.setTextSize(65);
		paint.setTextAlign(Align.CENTER);
		myCanvas.drawText(time, 80, 60, paint);
		return myBitmap;
	}
	
	static void updateAppWidget(Context context, AppWidgetManager awManager, int awId) {
		OASession session = OASession.getInstance(context);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);
		
		// TextView txtClock = (TextView) views.
		
		views.setTextViewText(R.id.txt_arrival, OASession.timeString(session.getArrival()));
		views.setTextViewText(R.id.txt_leaving, OASession.timeString(session.getLeaving()));
		views.setTextViewText(R.id.txt_left, OASession.timeString(session.getLeft()));
		
		awManager.updateAppWidget(awId, views);
	}
}