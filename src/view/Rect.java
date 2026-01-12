package view;
import java.awt.*;


/**
 * A square that can be manipulated and that draws itself on a canvas.
 *
 * @author launay
 * @version 2017.01.01
 *
 * @inv getWidth() == getHeight()
 */

public class Rect extends Figure
{

    /**
     * Create a new square.
     *
     * @param size the square initial size
     * @param x the square initial x location
     * @param y the square initial y location
     * @param color the square initial color.
     *
     * @pre size >= 0
     * @pre color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green")
     */
    public Rect(int width,int height, int x, int y, String color)
    {
        super(width,height, x, y, color);
    }
    
    public Rect(int width,int height, int x, int y, String color, boolean isVisible)
    {
        super(width,height, x, y, color, isVisible);
    }

    /**
     * Draw the square with current specifications on screen.
     */
    protected void draw()
    {
        Canvas canvas = Canvas.getCanvas();
        if(this.isVisible())
        	canvas.draw(this, getColor(), new Rectangle(getX(), getY(), getWidth(), getHeight()));
    }


}
