package com.jollyride.mhealth.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

public class CustomRouteView extends View {

    private Path path;
    private Paint routePaint;
    private Random random;

    // Route points
    private double pickupLat, pickupLng, destinationLat, destinationLng;
    private boolean hasPoints = false;

    // Driver location
    private double driverLat, driverLng;
    private boolean hasDriver = false;

    // Pin images
    private Drawable pickupDrawable;
    private Drawable destinationDrawable;
    private Drawable driverDrawable;

    public CustomRouteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setElevation(0f);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        init();
    }

    private void init() {
        path = new Path();
        random = new Random();

        routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routePaint.setColor(0xFF2F3C4C);   // default route color
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(12f);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    /* ---------------- Public APIs ---------------- */

    public void setRoutePoints(double pickupLat, double pickupLng,
                               double destinationLat, double destinationLng) {
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.hasPoints = true;
        invalidate();
    }

    public void setRoutePoints(LatLng pickup, LatLng destination) {
        if (pickup != null && destination != null) {
            setRoutePoints(pickup.latitude, pickup.longitude,
                    destination.latitude, destination.longitude);
        }
    }

    public void setLocations(double pickupLat, double pickupLng,
                             double destinationLat, double destinationLng) {
        setRoutePoints(pickupLat, pickupLng, destinationLat, destinationLng);
    }

    public void setLocations(LatLng pickup, LatLng destination) {
        setRoutePoints(pickup, destination);
    }

    public void updateDriverLocation(LatLng driver) {
        if (driver != null) {
            this.driverLat = driver.latitude;
            this.driverLng = driver.longitude;
            this.hasDriver = true;
            invalidate();
        }
    }

    public void setPickupDrawable(Drawable drawable) {
        this.pickupDrawable = drawable;
        invalidate();
    }

    public void setDestinationDrawable(Drawable drawable) {
        this.destinationDrawable = drawable;
        invalidate();
    }

    public void setDriverDrawable(Drawable drawable) {
        this.driverDrawable = drawable;
        invalidate();
    }

    /** -------- Added so Activities can call `setRoute(...)` -------- */
    public void setRouteColor(int color) {
        routePaint.setColor(color);
        invalidate();
    }

    public void setRoute(double pickupLat, double pickupLng,
                         double destinationLat, double destinationLng,
                         int color) {
        setRoutePoints(pickupLat, pickupLng, destinationLat, destinationLng);
        setRouteColor(color);
    }
    /* --------------------------------------------------------------- */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float startX = width * 0.1f;
        float startY = height * 0.3f;
        float endX = width * 0.9f;
        float endY = height * 0.8f;

        path.reset();
        path.moveTo(startX, startY);

        int segments = 4;
        float dx = (endX - startX) / segments;
        float dy = (endY - startY) / segments;

        for (int i = 1; i <= segments; i++) {
            float cx1 = startX + dx * (i - 0.5f);
            float cy1 = startY + dy * (i - 0.5f)
                    + random.nextInt((int) (height * 0.2f)) - height * 0.1f;
            float cx2 = startX + dx * (i - 0.2f);
            float cy2 = startY + dy * (i - 0.2f)
                    + random.nextInt((int) (height * 0.2f)) - height * 0.1f;
            float x = startX + dx * i;
            float y = startY + dy * i;

            path.cubicTo(cx1, cy1, cx2, cy2, x, y);
        }

        canvas.drawPath(path, routePaint);

        int pickupSize = dpToPx(32);
        int destSize = dpToPx(20);
        int driverSize = dpToPx(28);

        if (pickupDrawable != null) {
            int left = Math.round(startX - pickupSize / 2f);
            int top = Math.round(startY - pickupSize);
            pickupDrawable.setBounds(left, top, left + pickupSize, top + pickupSize);
            pickupDrawable.draw(canvas);
        }

        if (destinationDrawable != null) {
            int left = Math.round(endX - destSize / 2f);
            int top = Math.round(endY - destSize);
            destinationDrawable.setBounds(left, top, left + destSize, top + destSize);
            destinationDrawable.draw(canvas);
        }

        if (hasDriver && driverDrawable != null && hasPoints) {
            float t = computeRelativePositionFraction();
            float driverX = startX + (endX - startX) * t;
            float driverY = startY + (endY - startY) * t;

            int left = Math.round(driverX - driverSize / 2f);
            int top = Math.round(driverY - driverSize);
            driverDrawable.setBounds(left, top, left + driverSize, top + driverSize);
            driverDrawable.draw(canvas);
        }
    }

    private float computeRelativePositionFraction() {
        if (Math.abs(destinationLat - pickupLat) < 1e-9 &&
                Math.abs(destinationLng - pickupLng) < 1e-9) {
            return 0.5f;
        }

        double dx = destinationLng - pickupLng;
        double dy = destinationLat - pickupLat;
        double len2 = dx * dx + dy * dy;
        if (len2 == 0) return 0.5f;

        double px = driverLng - pickupLng;
        double py = driverLat - pickupLat;
        double proj = (px * dx + py * dy) / len2;

        return (float) Math.max(0, Math.min(1, proj));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
