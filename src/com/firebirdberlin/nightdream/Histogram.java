package com.firebirdberlin.nightdream;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;

class Histogram extends View {
      final private Handler handler = new Handler();
      private boolean AlarmSet = false;
      private boolean DayDreamMode = false;
      private boolean enabled = true;
      private boolean FingerDown;
      private boolean FingerDownDeleteAlarm;
      private Context ctx;
      private float tX, tY;
      private int customcolor = Color.parseColor("#33B5E5");
      private int customSecondaryColor = Color.parseColor("#C2C2C2");
      private int h;
      private int hour, min;
      private int w;
      private long AlarmTime = 0;
      private Paint paint = new Paint();
      private PendingIntent PendingAlarmIntent = null;
      private static AlarmManager am = null;
      private String nextAlarmFormatted = "";
      private Utility utility;
      public int touch_zone_radius = 150;

      long[] hist = new long[24];

      public Histogram(Context context, AttributeSet attrs) {
          super(context, attrs);
          ctx = context;

          if (am == null) {
              am = (AlarmManager)(ctx.getSystemService( Context.ALARM_SERVICE ));
          }

          for (int i = 0; i < 24; i++) hist[i] = 0;
      }

      public void setUtility(Utility u) {utility = u;}

      public void setCustomColor(int primary, int secondary) {
          customcolor = primary;
          customSecondaryColor = secondary;
      }

      public void setDaydreamMode(boolean onoff) {DayDreamMode = onoff;}

      public void setNextAlarmString(String s){
          nextAlarmFormatted = s;
          this.invalidate();
      }

      private float distance(Point a, Point b){
          return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
      }

      private Point getClickedPoint(MotionEvent e) {
          return new Point((int) e.getX(), (int) e.getY());
      }

      public boolean onTouchEvent(MotionEvent e) {
          if (utility.AlarmRunning()) utility.AlarmStop();

          if (nextAlarmFormatted.isEmpty() == false) {
                return handleClickForStockAlarm(e);
          }

          boolean eventCancelAlarm = handleAlarmCancelling(e);
          if (eventCancelAlarm) return true;

          boolean eventAlarmSet = handleAlarmSetEvents(e);
          if (eventAlarmSet) return true;
          return false;
      }

      private boolean handleAlarmSetEvents(MotionEvent e) {
          tX = e.getX();
          tY = e.getY();

          Point click = getClickedPoint(e);
          Point size = utility.getDisplaySize();

          Point ll = new Point(0, size.y); // lower left corner

          // set alarm clock
          switch (e.getAction()) {
              case MotionEvent.ACTION_DOWN:
                  // Allow start only in the top left corner
                  if (distance(click, ll) < touch_zone_radius) { // left corner
                      FingerDown = true;
                      AlarmSet = false;
                      removeAlarm();
                      XYtotime(tX,tY);
                      this.invalidate();
                      return true;
                  }
                  return false;
              case MotionEvent.ACTION_MOVE:
                  if (FingerDown == false) return false;
                  XYtotime(tX,tY);
                  this.invalidate();
                  break;
              case MotionEvent.ACTION_UP:
                  if (FingerDown == false) return false;
                  FingerDown = false;

                  XYtotime(tX,tY);

                  Calendar calendar = Calendar.getInstance();
                  long currentTimestamp = calendar.getTimeInMillis();
                  calendar.set(Calendar.HOUR_OF_DAY, hour);
                  calendar.set(Calendar.MINUTE, min);
                  calendar.set(Calendar.SECOND, 0);
                  AlarmTime = calendar.getTimeInMillis();
                  if (AlarmTime < currentTimestamp) AlarmTime += 86400000;
                  if (DayDreamMode == true){
                      long diffTimestamp = calendar.getTimeInMillis() - currentTimestamp;
                      //86400000 is a day
                      long myDelay = (diffTimestamp < 0 ? diffTimestamp + 86400000: diffTimestamp);
                      handler.postDelayed(setAlarmWhileRunning, myDelay);
                  } else{ // alarm is for the stand-alone app
                      // trigger alarm
                      removeAlarm();
                      Intent intent = new Intent("com.firebirdberlin.nightdream.WAKEUP");
                      intent.putExtra("cmd", "start alarm");
                      PendingAlarmIntent = PendingIntent.getBroadcast( ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                      am.set(AlarmManager.RTC_WAKEUP, AlarmTime, PendingAlarmIntent );
                  }
                  AlarmSet = true;
                  this.invalidate();
                  break;
          }
          return false;
      }

      private boolean handleClickForStockAlarm(MotionEvent e) {
          if(Build.VERSION.SDK_INT < 19) return false;

          Point click = getClickedPoint(e);
          Point size = utility.getDisplaySize();
          Point lc = new Point(size.x/2, size.y); // lower center

          switch (e.getAction()) {
              case MotionEvent.ACTION_DOWN:
                  if (distance(click, lc) < touch_zone_radius) { // bottom center
                      FingerDown = true;
                      this.invalidate();
                      return true;
                  }
                  break;
              case MotionEvent.ACTION_UP:
                  FingerDown = false;
                  this.invalidate();
                  if (distance(click, lc) < touch_zone_radius) { // bottom center
                      utility.openAlarmConfig();
                      return true;
                  }
                  break;
          }
          FingerDown = false;
          return false;
      }

      private boolean handleAlarmCancelling(MotionEvent e){
          Point click = getClickedPoint(e);
          Point size = utility.getDisplaySize();
          Point lr = new Point(size.x, size.y); // lower right corner

          // set alarm clock
          switch (e.getAction()) {
              case MotionEvent.ACTION_DOWN:
                  if (distance(click, lr) < touch_zone_radius) { // right corner
                      FingerDownDeleteAlarm = true;
                      this.invalidate();
                      return true;
                  }
                  return false;
              case MotionEvent.ACTION_UP:
                  if (FingerDownDeleteAlarm == true) {
                      AlarmSet = false;
                      FingerDownDeleteAlarm = false;
                      removeAlarm();
                      this.invalidate();
                      return true;
                  }
                  return false;
          }
          return false;
      }

      public void show(){
          enabled = true;
          this.invalidate();
      }

      public void hide(){
          enabled = false;
          this.invalidate();
      }

      public void count(int hour){
          if (hour >= 0 && hour < 24) {
              hist[hour]++;
              this.invalidate(); // force a redraw
          }
      }

      private void XYtotime(float x, float y) {
          Point size = utility.getDisplaySize();
          int w = size.x;
          int h = size.y;
          float hours = x/w * 24;
          float mins = (1.f - y/h) * 60;
          hour = (hours >= 24.f) ? 23 : (int) hours;
          min = (mins >= 60.f) ? 0 : (int) mins;
      }

      @Override
      protected void onDraw(Canvas canvas){
        Point size = utility.getDisplaySize();
        ColorFilter customColorFilter = new LightingColorFilter(customcolor, 1);
        ColorFilter secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);
        paint.setColorFilter(customColorFilter);

        int w = size.x;
        int h = size.y;
        if ( enabled && nextAlarmFormatted.isEmpty() ){
            // touch zones

            // set size of the touch zone
            if (size.x < size.y) touch_zone_radius = size.x/5;
            else touch_zone_radius = size.y/5;
            touch_zone_radius = (touch_zone_radius > 180) ? 180 : touch_zone_radius;

            int tzr2 = touch_zone_radius - (int) (0.07 * touch_zone_radius);
            int tzr3 = touch_zone_radius - (int) (0.14 * touch_zone_radius);

            // left corner
            paint.setColor(Color.WHITE);
            if (FingerDown == true) paint.setAlpha(255);
            else paint.setAlpha(153);

            canvas.drawCircle(0, h, touch_zone_radius, paint);

            paint.setColor(Color.BLACK);
            //paint.setAlpha(102);
            canvas.drawCircle(0, h, tzr2, paint);

            paint.setColor(Color.WHITE);
            if (FingerDown == true) paint.setAlpha(153);
            else paint.setAlpha(102);

            canvas.drawCircle(0, h, tzr3, paint);

            // right corner
            if (AlarmSet == true || FingerDown){
                paint.setColor(Color.WHITE);
                if (FingerDownDeleteAlarm == true) paint.setAlpha(255);
                else paint.setAlpha(153);
                canvas.drawCircle(w, h, touch_zone_radius, paint);

                paint.setColor(Color.BLACK);
                canvas.drawCircle(w, h, tzr2, paint);

                paint.setColor(Color.WHITE);
                if (FingerDownDeleteAlarm == true) paint.setAlpha(153);
                else paint.setAlpha(102);

                canvas.drawCircle(w, h, tzr3, paint);
            }

            if (utility.isDebuggable()){
                // histogram
                long max = hist[0];
                for (int i = 0; i < 24; i++){
                    if (hist[i] > max ) max = hist[i];
                }
                // prevent divide by zero
                if (max > 0){

                    int mw = (w-48)/23;
                    paint.setColor(Color.parseColor("#AA004666"));
                    paint.setStrokeWidth(1);

                    long sc = h/2/max;
                    for (int i = 0; i < 24 ; i++){
                        canvas.drawRect(1+i*mw, h-sc*hist[i], 1+ (i+1)*mw - 1, h, paint);
                    }
                }
            }

        }

        Resources res = getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.ic_alarmclock);
        paint.setColorFilter(secondaryColorFilter);

        if ( nextAlarmFormatted.isEmpty() ){
            if (FingerDown == true || AlarmSet == true){
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, min);
                calendar.set(Calendar.SECOND, 0);

                String l = getTimeFormatted(calendar);

                paint.setTextSize(touch_zone_radius * .6f);
                float lw = paint.measureText(l);
                float cw = touch_zone_radius-60;
                if ((touch_zone_radius) <= 100)  cw = 0;
                if (FingerDown || AlarmSet){
                    paint.setColor(Color.WHITE);
                    canvas.drawText(l, w/2-(lw+cw)/2 + cw, h-touch_zone_radius/3, paint );
                }

                if ((touch_zone_radius) > 100){ // no image on on small screens
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, touch_zone_radius-60, touch_zone_radius-60, false);
                    canvas.drawBitmap(resizedBitmap, w/2 - (lw+cw)/2 - cw/2, h-touch_zone_radius+30, paint);
                }
            }
        } else { // next upcoming alarm is set
            float lw = paint.measureText(nextAlarmFormatted);
            float cw = touch_zone_radius - 60;
            paint.setTextSize(touch_zone_radius*.6f);
            paint.setColor(Color.WHITE);
            paint.setAlpha((FingerDown) ? 103 : 153);
            canvas.drawText(nextAlarmFormatted, w/2 - (lw + cw)/2 + cw, h - touch_zone_radius/3, paint );
            if ((touch_zone_radius) > 100){ // no image on on small screens
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, touch_zone_radius-60, touch_zone_radius-60, false);
                canvas.drawBitmap(resizedBitmap, w/2 - (lw+cw)/2 - cw/2, h-touch_zone_radius+30, paint);
            }
        }
      }

      private String getTimeFormatted(Calendar calendar) {
          String localPattern  = "";
          if (Build.VERSION.SDK_INT >= 18){
              if (is24HourFormat(ctx)) {
                  localPattern = getBestDateTimePattern(Locale.getDefault(), "HH:mm");
              } else {
                  localPattern = getBestDateTimePattern(Locale.getDefault(), "hh:mm a");
              }
          } else {
              DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
              localPattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
          }

          SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
          return hourDateFormat.format(calendar.getTime());
      }

      public void storeData() {
          RandomAccessFile out = null;

          String filePath = ctx.getFilesDir().getPath().toString() + "/usage.dat";
          try {
              out = new RandomAccessFile(filePath, "rw");
              FileChannel file = out.getChannel();
              ByteBuffer buf = file.map(FileChannel.MapMode.READ_WRITE, 0, Long.SIZE * hist.length);
              for (long i : hist) {
                  buf.putLong(i);
              }
              file.close();
              out.close();
          } catch (IOException e) {
              throw new RuntimeException(e);
          } finally {

          }
      }

      public void restoreData() {
          RandomAccessFile in = null;
          String filePath = ctx.getFilesDir().getPath().toString() + "/usage.dat";
          try {
              in = new RandomAccessFile(filePath, "rw");
              FileChannel file = in.getChannel();
              ByteBuffer buf = file.map(FileChannel.MapMode.READ_WRITE, 0, Long.SIZE * hist.length);
              for (int i = 0; i < hist.length; i++){
                  hist[i] = buf.getLong();
              }
              file.close();
              in.close();
          } catch (FileNotFoundException e) {
              // ignore if file not found
          } catch (IOException e) {
              throw new RuntimeException(e);
          } finally {

          }
      }

      public int getAlarmHour(){return hour;}
      public int getAlarmMinutes(){return min;}

      public boolean isAlarmSet(){return AlarmSet;}
      public long getAlarmTimeMillis(){return AlarmTime;}

      public void startAlarm(){
          AlarmSet = true; // because we want to fire it now !
          handler.post(setAlarmWhileRunning);
      }

      public void stopAlarm(){
          handler.post(stopRunningAlarm);
      }

      public void removeAlarm(){
          if (DayDreamMode == true) {
              if (setAlarmWhileRunning != null) {
                  handler.removeCallbacks(setAlarmWhileRunning);
              }
          } else {
              if (PendingAlarmIntent !=null){
                  am.cancel(PendingAlarmIntent);
                  PendingAlarmIntent = null;
              }
          }
          AlarmSet = false;
      }

      private Runnable setAlarmWhileRunning = new Runnable() {
          @Override
          public void run() {
              if (isAlarmSet()){
                  try{
                      utility.AlarmPlay();
                  } catch (Exception e){}
                  AlarmSet = false;
                  handler.postDelayed(stopRunningAlarm, 120000); // stop it after 2 mins
              }
          }
      };

      private Runnable stopRunningAlarm = new Runnable() {
          @Override
          public void run() {
              utility.AlarmStop();
              AlarmSet = false;
              invalidate();
          }
      };

}
