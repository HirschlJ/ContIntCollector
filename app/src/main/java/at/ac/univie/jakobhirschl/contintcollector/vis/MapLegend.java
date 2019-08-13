package at.ac.univie.jakobhirschl.contintcollector.vis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

/**
 * Created by Jakob Hirschl on 07.12.2015.
 */
public class MapLegend extends View
{
    private int startPos;
    private int endPos;
    private int posRight;
    public MapLegend(Context context, int startPos, int endPos, int posRight)
    {
        super(context);
        this.startPos = startPos;
        this.endPos = endPos;
        this.posRight = posRight;
        Log.i("MapLegend", startPos + " " + endPos + " " + posRight);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        //draw legend of map
        super.onDraw(canvas);
        float lineRatio = ((float)startPos-endPos)/510.0f;
        float start = startPos;
        int green =  255;
        int red = 0;
        Paint paint;
        for(int i = 0; i < 510; i++)
        {
            if(i < 255)
            {
                red++;
            }
            else
            {
                green--;
            }
            paint = new Paint();
            paint.setColor(Color.rgb(red,green,0));
            paint.setStrokeWidth(20.0f);
            canvas.drawLine(posRight, start, posRight, start-lineRatio, paint);
            start-=lineRatio;
        }
        /*
        Paint paint = new Paint();
        paint.setColor(Color.rgb(255,0,0));
        paint.setStrokeWidth(20.0f);
        canvas.drawLine(0, 0, 100, 100, paint);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(20.0f);
        canvas.drawLine(100, 100, 200, 200, paint);*/
    }
}
