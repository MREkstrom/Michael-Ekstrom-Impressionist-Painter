package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;

/**
 * Created by jon on 3/20/2016.
 * Later modified by Michael Ekstrom
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private Paint _backGroundPaint = new Paint();

    private int _alpha = 150; //Hard coded alpha value
    private float _speed = 0; //Current brush speed for use in some brush strokes
    private int _rotate = 0; //Current rotation for use in some brush strokes
    private Point _lastPoint = null; //Used to calculate _speed
    private boolean _specialMode = false;//When true, invert colors
    private boolean _backGroundOn = false;//When true, draw the image behind the _offScreenBitmap
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);
        _paint.setTextSize(100f);//For use in BrushType.Letter

        _backGroundPaint.setAlpha(255);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);

        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_backGroundOn) {
            canvas.drawBitmap(_imageView.getDrawingCache(), 0, 0, _backGroundPaint);
        }

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    //Toggle whether or not color inversion is in use
    public boolean toggleSpecialFeature(){
        _specialMode = !_specialMode;
        return _specialMode;
    }

    //Toggle whether or not the actual image shows up in the background
    public void backGroundOn(){
        _backGroundOn = true;
        invalidate();
    }

    //Toggle whether or not the actual image shows up in the background
    public void backGroundOff(){
       _backGroundOn = false;
        invalidate();
    }

    //Saves the image to the device. Can either save with or without the image as a background
    public Bitmap save(boolean withBackground){
        if (_imageView.getDrawingCache() == null)
            return null;
        //Create a new bitmap
        Bitmap bmp = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);

        //Draw the current _offScreenBitmap and the background (if requested) to the bitmap
        if (withBackground)
            canvas.drawBitmap(_imageView.getDrawingCache(), 0, 0, _backGroundPaint);
        if(_offScreenBitmap != null)
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);

        return bmp;

    }

    //Extract the color from the source bitmap at the given X and Y position
    public void setPixelColor(float x, float y) {

        Bitmap bmp = _imageView.getDrawingCache();
        int pixel = bmp.getPixel((int) x, (int) y);

        int r = Color.red(pixel);
        int b = Color.blue(pixel);
        int g = Color.green(pixel);

        //If color inversion is on, invert colors
        if (_specialMode) {
            r = 255 - r;
            b = 255 - b;
            g = 255 - g;
        }

        _paint.setARGB(_alpha, r, g, b);
    }

    //Draws at the given location with the current brush option and paint color
    public void drawSwatch(float x, float y) {
        switch(_brushType) {
            case Circle:
                //Adjust the size and the center of the circle based on random values and brush speed
                int circlesize = (int) (10 * _speed);
                float xadjust = (float) ((Math.random()-.5)* _speed *5);
                float yadjust = (float) ((Math.random()-.5)* _speed *5);
                _offScreenCanvas.drawCircle(x+xadjust, y+yadjust, circlesize, _paint);
                break;
            case Square:

                //Rotate code found on
                //http://stackoverflow.com/questions/19837489/android-how-to-rotate-rect-object
                _offScreenCanvas.save();
                _offScreenCanvas.rotate(_rotate, x, y);
                int halfrect = 50; //Half of the rectangle's size
                _offScreenCanvas.drawRect(x-halfrect, y-halfrect, x+halfrect, y+halfrect, _paint);
                _offScreenCanvas.restore();

                //Rotate the canvas more each time
                _rotate += 1;
                if (_rotate == 360)
                    _rotate = 0; //Avoid extremely improbable integer overflow edge case
                break;

            case Letter:
                //Pick a random letter from A to Z and draw it
                int randval = (int) (Math.random() * 26) + 65;
                _offScreenCanvas.drawText(Character.toString((char) randval) ,x, y, _paint);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        float currTouchX = motionEvent.getX();
        float currTouchY = motionEvent.getY();

        Rect bmpPos = getBitmapPositionInsideImageView(_imageView);

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                //Set _lastPoint for speed calculations
                _lastPoint = new Point((int)motionEvent.getX(), (int)motionEvent.getY());
                _speed = 2f; //messes up things if this is zero

                //If they tried to draw outside, return true and do not draw
                if (! bmpPos.contains((int)currTouchX, (int)currTouchY))
                    return true;
                setPixelColor(currTouchX, currTouchY);
                drawSwatch(currTouchX, currTouchY);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                //Movement history code from in-class example
                int historySize = motionEvent.getHistorySize();

                //Use historical values for smoother brush strokes and more accurate speed calculations
                for (int i = 0; i < historySize; i++) {
                    if (_brushType == BrushType.Letter && i%4 != 0)
                        continue;
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    //Calculate the speed for use in brush strokes.
                    //Technically distance, but we are scaling the numbers in the drawSwatch method anyways
                    //Only compute speed if the brushType depends on it
                    if (_brushType == BrushType.Circle)
                    _speed = (float) Math.sqrt(Math.pow(_lastPoint.x - touchX,2) + Math.pow(_lastPoint.y - touchY,2));
                    _lastPoint = new Point((int)touchX, (int)touchY);

                    //If they tried to draw outside, do not draw or request color.
                    if (! bmpPos.contains((int)touchX, (int)touchY))
                        continue;
                    setPixelColor(touchX, touchY);
                    drawSwatch(touchX, touchY);
                }

                //Calculate speed again
                if (_brushType == BrushType.Circle)
                    _speed = (float) Math.sqrt(Math.pow(_lastPoint.x - currTouchX,2) + Math.pow(_lastPoint.y - currTouchY,2));
                _lastPoint = new Point((int) currTouchX, (int) currTouchY);

                //If they tried to draw outside, return true and do not draw
                if (! bmpPos.contains((int)currTouchX, (int)currTouchY))
                    return true;
                setPixelColor(currTouchX, currTouchY);
                drawSwatch(currTouchX, currTouchY);


                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }


        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

