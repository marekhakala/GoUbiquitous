package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private static String TAG = SunshineWatchFaceService.class.getSimpleName();

    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private static final String WEATHER_PATH = "/weather";
        private static final String WEATHER_INFO_PATH = "/weather-info";

        private static final String KEY_UUID = "uuid";
        private static final String KEY_HIGH = "high";
        private static final String KEY_LOW = "low";
        private static final String KEY_WEATHER_ID = "weatherId";

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        boolean mRegisteredTimeZoneReceiver = false;
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            }
        };

        Paint mBackgroundPaint;
        Paint mTextTimePaint;

        Paint mTextDatePaint;
        Paint mTextTempHighPaint;
        Paint mTextTempLowPaint;

        Paint mTextDateAmbientPaint;
        Paint mTextTempLowAmbientPaint;

        String mWeatherHigh;
        String mWeatherLow;
        Bitmap mWeatherIcon;

        float mTimeYOffset;
        float mDateYOffset;
        float mWeatherYOffset;
        float mDividerYOffset;

        boolean isAmbientMode;
        boolean mLowBitAmbient;
        private Calendar mCalendar;
        private Resources mResources;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mResources = SunshineWatchFaceService.this.getResources();
            mTimeYOffset = mResources.getDimension(R.dimen.digital_time_text_y_offset);
            mDateYOffset = mResources.getDimension(R.dimen.digital_date_text_y_offset);
            mWeatherYOffset = mResources.getDimension(R.dimen.digital_weather_y_offset);
            mDividerYOffset = mResources.getDimension(R.dimen.digital_divider_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(getBaseContext(), R.color.digital_background));

            mTextTimePaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_time), NORMAL_TYPEFACE);
            mTextDatePaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_date), NORMAL_TYPEFACE);
            mTextDateAmbientPaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_date_ambient), NORMAL_TYPEFACE);

            mTextTempHighPaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_temp_high), BOLD_TYPEFACE);
            mTextTempLowPaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_temp_low), NORMAL_TYPEFACE);
            mTextTempLowAmbientPaint = createTextPaint(
                    ContextCompat.getColor(getBaseContext(), R.color.digital_text_temp_low), NORMAL_TYPEFACE);

            mCalendar = Calendar.getInstance();
        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                mCalendar.setTimeZone(TimeZone.getDefault());
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            updateTimer();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();

            float timeTextSize = resources.getDimension(isRound ? R.dimen.digital_time_text_size_round : R.dimen.digital_time_text_size);
            mTextTimePaint.setTextSize(timeTextSize);

            float dateTextSize = resources.getDimension(isRound ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            mTextDatePaint.setTextSize(dateTextSize);
            mTextDateAmbientPaint.setTextSize(dateTextSize);

            float tempTextSize = resources.getDimension(isRound ? R.dimen.digital_temp_text_size_round : R.dimen.digital_temp_text_size);
            mTextTempHighPaint.setTextSize(tempTextSize);
            mTextTempLowAmbientPaint.setTextSize(tempTextSize);
            mTextTempLowPaint.setTextSize(tempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (isAmbientMode != inAmbientMode) {
                isAmbientMode = inAmbientMode;

                if (mLowBitAmbient) {
                    mTextTimePaint.setAntiAlias(!isAmbientMode);
                    mTextDatePaint.setAntiAlias(!isAmbientMode);
                    mTextDateAmbientPaint.setAntiAlias(!isAmbientMode);
                    mTextTempHighPaint.setAntiAlias(!isAmbientMode);
                    mTextTempLowAmbientPaint.setAntiAlias(!isAmbientMode);
                    mTextTempLowPaint.setAntiAlias(!isAmbientMode);
                }
                invalidate();
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver)
                return;

            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            if (isAmbientMode)
                canvas.drawColor(Color.BLACK);
            else
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            mCalendar.setTimeInMillis(System.currentTimeMillis());

            String timeText;
            boolean isTwentyFourHours = DateFormat.is24HourFormat(getBaseContext());

            int amPm = mCalendar.get(Calendar.AM_PM);
            int minutes = mCalendar.get(Calendar.MINUTE);
            int seconds = mCalendar.get(Calendar.SECOND);

            if (isTwentyFourHours) {
                String timeTextFormat = "%02d:%02d";

                if (isAmbientMode)
                    timeText = String.format(timeTextFormat,
                            mCalendar.get(Calendar.HOUR_OF_DAY), minutes);
                else
                    timeText = String.format(timeTextFormat + ":%02d",
                            mCalendar.get(Calendar.HOUR_OF_DAY), minutes, seconds);
            } else {
                String amPmText = Utilities.getAmPm(getResources(), amPm);
                String timeTextFormat = isAmbientMode ? "%d:%02d %s" : "%d:%02d:%02d %s";
                int hours = (mCalendar.get(Calendar.HOUR) == 0) ? 12 : mCalendar.get(Calendar.HOUR);

                if (isAmbientMode)
                    timeText = String.format(timeTextFormat, hours, minutes, amPmText);
                else
                    timeText = String.format(timeTextFormat, hours, minutes, seconds, amPmText);
            }

            float xOffsetTime = (mTextTimePaint.measureText(timeText) / 2);
            canvas.drawText(timeText, bounds.centerX() - xOffsetTime, mTimeYOffset, mTextTimePaint);

            Paint datePaint = isAmbientMode ? mTextDateAmbientPaint : mTextDatePaint;

            String monthOfYear = Utilities.getMonthOfYear(getResources(), mCalendar.get(Calendar.MONTH));
            String dayOfWeek = Utilities.getDayOfWeek(getResources(), mCalendar.get(Calendar.DAY_OF_WEEK));

            int year = mCalendar.get(Calendar.YEAR);
            int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);

            String dateText = String.format("%s, %s %d %d", dayOfWeek, monthOfYear, dayOfMonth, year);
            float xOffsetDate = datePaint.measureText(dateText) / 2;
            canvas.drawText(dateText, bounds.centerX() - xOffsetDate, mDateYOffset, datePaint);
            canvas.drawLine(bounds.centerX() - 20, mDividerYOffset, bounds.centerX() + 20, mDividerYOffset, datePaint);

            if (mWeatherHigh != null && mWeatherLow != null) {
                canvas.drawLine(bounds.centerX() - 20, mDividerYOffset, bounds.centerX() + 20, mDividerYOffset, datePaint);

                float highTextLen = mTextTempHighPaint.measureText(mWeatherHigh);

                if (isAmbientMode) {
                    float lowTextLen = mTextTempLowAmbientPaint.measureText(mWeatherLow);
                    float xOffset = bounds.centerX() - ((highTextLen + lowTextLen + 20) / 2);

                    canvas.drawText(mWeatherHigh, xOffset, mWeatherYOffset, mTextTempHighPaint);
                    canvas.drawText(mWeatherLow, xOffset + highTextLen + 20, mWeatherYOffset, mTextTempLowAmbientPaint);
                } else {
                    float xOffset = bounds.centerX() - (highTextLen / 2);
                    canvas.drawText(mWeatherHigh, xOffset, mWeatherYOffset, mTextTempHighPaint);
                    canvas.drawText(mWeatherLow, bounds.centerX() + (highTextLen / 2) + 20, mWeatherYOffset, mTextTempLowPaint);

                    float iconXOffset = bounds.centerX() - ((highTextLen / 2) + mWeatherIcon.getWidth() + 30);
                    canvas.drawBitmap(mWeatherIcon, iconXOffset, mWeatherYOffset - mWeatherIcon.getHeight(), null);
                }
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning())
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }

        private void handleUpdateTimeMessage() {
            invalidate();

            if (shouldTimerBeRunning())
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME,
                        INTERACTIVE_UPDATE_RATE_MS - (System.currentTimeMillis() % INTERACTIVE_UPDATE_RATE_MS));
        }

        @Override
        public void onConnected(Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            requestWeatherInfo();
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
        }

        @Override
        public void onConnectionSuspended(int id) {
        }

        @Override
        public void onDataChanged(DataEventBuffer events) {
            for (DataEvent event : events) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {

                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    String path = event.getDataItem().getUri().getPath();
                    Log.d(TAG, "PATH: " + path);

                    if (path.equals(WEATHER_INFO_PATH)) {
                        setWeatherHigh(dataMap);
                        setWeatherLow(dataMap);
                        setCurrentWeather(dataMap);

                        invalidate();
                    }
                }
            }
        }

        private void setWeatherHigh(DataMap map) {
            if (map.containsKey(KEY_HIGH)) {
                mWeatherHigh = map.getString(KEY_HIGH);
                Log.d(TAG, "High: " + mWeatherHigh);
            } else
                Log.d(TAG, "High: ERROR");
        }

        private void setWeatherLow(DataMap map) {
            if (map.containsKey(KEY_LOW)) {
                mWeatherLow = map.getString(KEY_LOW);
                Log.d(TAG, "Low: " + mWeatherLow);
            } else
                Log.d(TAG, "Low: ERROR");
        }

        private void setCurrentWeather(DataMap map) {
            if (map.containsKey(KEY_WEATHER_ID)) {
                Drawable drawableIcon = ResourcesCompat.getDrawable(getResources(),
                        Utilities.getIconResourceForWeatherCondition(map.getInt(KEY_WEATHER_ID)), null);
                Bitmap icon = ((BitmapDrawable) drawableIcon).getBitmap();
                float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
                mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);
            } else
                Log.d(TAG, "Weather: ERROR");
        }

        public void requestWeatherInfo() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
            putDataMapRequest.getDataMap().putString(KEY_UUID, UUID.randomUUID().toString());
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {

                        @Override
                        public void onResult(DataApi.DataItemResult itemResult) {
                            if (!itemResult.getStatus().isSuccess())
                                Log.d(TAG, "Asking for the weather data failed.");
                            else
                                Log.d(TAG, "Successfully asked for the weather data.");
                        }
                    });
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message message) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();

            if (engine != null) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
