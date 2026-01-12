package view;

/**
 * An abstract figure that can be manipulated and that draws itself on a canvas.
 *
 * @author launay
 * @version 2017.01.01
 *
 * @inv getWidth() >= 0 && getHeight() >= 0
 * @inv getColor().equals("white") || getColor().equals("black") || getColor().equals("red") || getColor().equals("blue") || getColor().equals("yellow") || getColor().equals("green")
 */
public abstract class Figure
{
    private int width; // the figure width in pixels
    private int height; // the figure height in pixels
    private int x; // the figure x location in pixels
    private int y; // the figure y location in pixels
    private String color = "white"; // the figure color. Valid colors are "white", "black", "red", "yellow", "blue", "green".
    private boolean isVisible;
    
    /**
     * Create a new figure.
     *
     * @param width the figure initial width
     * @param height the figure initial height
     * @param x the figure initial x location
     * @param y the figure initial y location
     * @param color the figure initial color.
     *
     * @pre width >= 0 && height >= 0
     * @pre color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green")
     */
    public Figure(int width, int height, int x, int y, String color)
    {
        assert width >= 0 && height >= 0 : "Wrong dimensions";
        assert color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green") : "Wrong color";
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.color = color;
        invariant();
        isVisible = true;
    }
    
    public Figure(int width, int height, int x, int y, String color, boolean isVisible) {
    	this(width, height, x, y, color);
    	this.isVisible = isVisible;
    }
    
    public boolean isVisible() {
    	return isVisible;
    }
    
    public void setVisible(boolean isVisible) {
    	this.isVisible = isVisible;
    }

    /**
     * Check whether the figure is inside the canvas
     *
     * @return true if the figure is inside the canvas
     */
    public boolean isInside()
    {
        return x >= 0 && x+width < Canvas.WIDTH && y >= 0 && y+height < Canvas.HEIGHT;
    }

    /**
     * Give the figure x location in pixels
     *
     * @return the figure x location in pixels
     */
    public int getX() {
        return x;
    }

    /**
     * Give the figure y location in pixels
     *
     * @return the figure y location in pixels
     */
    public int getY() {
        return y;
    }

    /**
     * Give the figure width in pixels
     *
     * @return the figure width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     *Give the figure height in pixels
     *
     * @return the figure height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Give the figure color
     *
     * @return the figure color
     */
    public String getColor() {
        return color;
    }

    /**
     * Move the figure in an autonomous way
     */
    public void move()
    {
        // do nothing
        invariant();
    }

    /**
     * Move the figure by 'dx' and 'dy' pixels.
     *
     * @param dx number of pixels to move to the right (distance>0) or left (distance<0).
     * @param dy number of pixels to move to the bottom (distance>0) or top (distance<0).
     */
    public void move(int dx, int dy)
    {
        erase();
        x += dx;
        y += dy;
        draw();
        invariant();
    }

    /**
     * Change the size to the new size (in pixels).
     *
     * @param width the new width in pixels
     * @param height the new height in pixels
     *
     * @pre width >= 0 && height >= 0
     */
    public void setSize(int width, int height)
    {
        assert width >= 0 && height >= 0 : "Wrong dimensions";
        erase();
        this.width = width;
        this.height = height;
        draw();
        invariant();
    }

    /**
     * Change the color.
     *
     * @param color the new color
     *
     * @pre color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green")
     */
    public void setColor(String color)
    {
        assert color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green") : "Wrong color";
        this.color = color;
        draw();
        invariant();
    }

    /**
     * Draw the figure with current specifications on screen.
     */
    protected abstract void draw();

    /**
     * Erase the figure on screen.
     */
    protected void erase()
    {
        Canvas canvas = Canvas.getCanvas();
        canvas.erase(this);
    }

    /**
     * Check the class invariant
     */
    protected void invariant() {
        assert width >= 0 && height >= 0 : "Invariant violated: wrong dimensions";
        assert color.equals("white") || color.equals("black") || color.equals("red") || color.equals("blue") || color.equals("yellow") || color.equals("green") : "Invariant violated: wrong color";
    }
}
