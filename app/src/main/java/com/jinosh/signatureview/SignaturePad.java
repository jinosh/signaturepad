package com.jinosh.signatureview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.administrator.signatureview.R;

/**
 * Created by jinosh on 19/7/17.
 * Create a custom view to record-signature
 */


public class SignaturePad extends View {

    private static final float STROKE_WIDTH = 5f;
    /**
     * Need to track this so the dirty region can accommodate the stroke.
     **/
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
    private final RectF dirtyRect = new RectF();
    private Paint paint = new Paint();
    private Path path = new Path();
    /**
     * Optimizes painting by invalidating the smallest possible area.
     */
    private float lastTouchX;
    private float lastTouchY;
    private float signatureStroke;
    private int signatureColor;
    //a listener which listen for signature capture
    private OnSignatureListener onSignatureListener;
    private boolean mIsEmpty;

    public SignaturePad(Context context) {
        super(context);
        initSignaturePad(context, null);
    }

    public SignaturePad(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSignaturePad(context, attrs);

    }

    private void initSignaturePad(Context context, AttributeSet attributeSet) {
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.SignaturePad);
        signatureColor = typedArray.getColor(R.styleable.SignaturePad_sign_pen_color, Color.parseColor("#000000"));
        signatureStroke = typedArray.getFloat(R.styleable.SignaturePad_sign_pen_stroke, 8f);
        paint.setAntiAlias(true);
        paint.setColor(signatureColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(signatureStroke);
    }

    /*
     Set last signature in signature view. This method is called when onRestoreInstanceState invoked.
     */
    public void setLastSignBitmap(final Bitmap lastSignBitmap) {

        //check whether the view is attached atleast once or not
        if (ViewCompat.isLaidOut(this)) {
            clearSignaturePad();
            Bitmap signatureBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);

            RectF tempSrc = new RectF();
            RectF tempDst = new RectF();

            int dWidth = lastSignBitmap.getWidth();
            int dHeight = lastSignBitmap.getHeight();
            int vWidth = getWidth();
            int vHeight = getHeight();

            // Generate the required transform.
            tempSrc.set(0, 0, dWidth, dHeight);
            tempDst.set(0, 0, vWidth, vHeight);

            Matrix drawMatrix = new Matrix();
            drawMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);

            Canvas canvas = new Canvas(signatureBitmap);
            canvas.drawBitmap(lastSignBitmap, drawMatrix, null);
            setIsEmpty(false);
            invalidate();
        } else {
            //else wait for the view to attach and then call setLastSignBitmap
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Remove observer
                    if (Build.VERSION.SDK_INT >= 16) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    // Get last Signature bitmap...
                    setLastSignBitmap(lastSignBitmap);
                }
            });
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    /**
     * Erases the signature.
     */
    public void clearSignaturePad() {
        path.reset();
        // clear current signature
        invalidate();
        if (onSignatureListener != null)
            onSignatureListener.onClear();

        setIsEmpty(true);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * points.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX;
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX;
        }
        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY;
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     */
    private void resetDirtyRect(float eventX, float eventY) {
        // The lastTouchX and lastTouchY were set when the ACTION_DOWN
        // motion event occurred.
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(eventX, eventY);
                lastTouchX = eventX;
                lastTouchY = eventY;
                //set listener onStartSigning to indicate signature capture started
                if (onSignatureListener != null)
                    onSignatureListener.onStartSigning();
                // There is no end point yet, so don't waste cycles invalidating.
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                // Start tracking the dirty region.
                resetDirtyRect(eventX, eventY);
                // When the hardware tracks events faster than they are delivered, the
                // event will contain a history of those skipped points.
                int historySize = event.getHistorySize();
                for (int i = 0; i < historySize; i++) {
                    float historicalX = event.getHistoricalX(i);
                    float historicalY = event.getHistoricalY(i);
                    expandDirtyRect(historicalX, historicalY);
                    path.lineTo(historicalX, historicalY);
                }
                // After replaying history, connect the line to the touch point.
                path.lineTo(eventX, eventY);
                setIsEmpty(false);
                break;
            default:
                return false;
        }
        // Include half the stroke width to avoid clipping.
        invalidate(
                (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));
        lastTouchX = eventX;
        lastTouchY = eventY;
        return true;
    }

    /**
     * Get signature as bitmap
     *
     * @return Bitmap
     */
    public Bitmap getSignatureBitmap() {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = this.getBackground();
        if (bgDrawable != null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        this.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    /**
     * Set Signature Capture Listener
     *
     * @param onSignatureListener {@link OnSignatureListener} object
     * @return void
     */
    public void setOnSignatureListener(OnSignatureListener onSignatureListener) {
        this.onSignatureListener = onSignatureListener;
    }

    /**
     * Check whether signaturePad is empty or not
     *
     * @param newValue boolean value
     */
    private void setIsEmpty(boolean newValue) {
        mIsEmpty = newValue;
        if (onSignatureListener != null) {
            if (mIsEmpty) {
                onSignatureListener.onClear();
            } else {
                onSignatureListener.onSigned();
            }
        }
    }


    //interface which listen for signature capturing process
    public interface OnSignatureListener {
        void onStartSigning();

        void onSigned();

        void onClear();
    }
}