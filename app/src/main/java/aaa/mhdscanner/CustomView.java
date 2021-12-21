package aaa.mhdscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import static android.content.Context.MODE_PRIVATE;

public class CustomView extends LinearLayout {
    private Bitmap bitmap;

    private TextureView mTextureView;


    public CustomView(Context context){
        super(context);
    }

    public CustomView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);

        if(bitmap == null){
            createWindowFrame();
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    protected void createWindowFrame(){
        mTextureView = findViewById(R.id.texture);

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(bitmap);
        //links oben rechts unten

        float cropFactor = (float) getCropFactor(getContext())/100000;
        Log.v("Debug10", "Crop Faktor has been read: " + cropFactor);

        Log.v("Debug6", "outer Rectangle: " + 0 + ", " + 0 + ", " + getWidth() + ", " + getHeight());
        RectF outerRectangle = new RectF(0,0,getWidth(), getHeight());

        float DesW = (float) getLastDesiredWidth(getContext());
        float DesH = (float) getLastDesiredHeight(getContext());
        float Ws = (float) getLastWidth(getContext());
        float Hs = (float) getLastHeight(getContext());
        Log.v("Debug6", "DesiredWidth: " + DesW + " DesiredHeight: " + DesH + " LastWidth: " + Ws + " Last Height " + Hs);

        float breiteTransp = getWidth()*(1-DesW/(Ws*cropFactor))/2;
        Log.v("Debug6", "Berechnete Breite Transparenz: " + breiteTransp);
        float hoeheTransp = getHeight()*(1-DesH/(Hs*cropFactor))/2;
        Log.v("Debug6", "Berechnete Höhe Transparenz: " + hoeheTransp);
        //links oben rechts unten
        float right = getWidth()-breiteTransp;
        float bottom = getHeight() - hoeheTransp;
        Log.v("Debug6", "Rectangle: " + breiteTransp + ", " + hoeheTransp + ", " + right + ", " + bottom);
        RectF innerRectangle = new RectF( breiteTransp, hoeheTransp, right, bottom);

        //RectF innerRectangle = new RectF(mTextureView.getWidth() * .00f, mTextureView.getHeight() * .20f, mTextureView.getWidth() * .90f, mTextureView.getHeight() * .70f); // * .10f,
        //RectF innerRectangle = new RectF(getWidth() * .30781f, getHeight() * .44737f, getWidth() * .69219f, getHeight() * .55263f); // * .10f,

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = ContextCompat.getColor(getContext(), R.color.black);
        paint.setColor(color);
        paint.setAlpha(150);
        osCanvas.drawRect(outerRectangle, paint);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        osCanvas.drawRect(innerRectangle, paint);
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b){
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }

    static public int getCropFactor(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("CropFactor", 100000);
    }

    static public int getLastDesiredWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesWidth", 100);
    }

    static public int getLastDesiredHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesHeight", 100);
    }

    static public int getLastWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastWidth", 0);
    }

    static public int getLastHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastHeight", 0);
    }
}

