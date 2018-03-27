/**
 * Created by rudi on 23.06.2017.
 */

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

import javax.swing.*;
import javax.swing.event.MouseInputListener;

public class PanelObject extends ImageCanvas implements MouseInputListener,ComponentListener {


    Graphics2D graph2d;
    BufferedImage bufor;
    Image img;
    Rectangle2D rectField;
    Point startDrag , endDrag;

    public PanelObject(ImagePlus imp) {
        super(imp);
        img = imp.getImage();
      //  startDrag = getMousePosition();

    }

    @Override
    public void mouseClicked(MouseEvent e) {

        System.out.println("Clicked in X: " + e.getX() + " Y: " + e.getY());
        Main.jInfo.setText("Clicked in X: " + e.getX() + " Y: " + e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
       // graph2d.setColor(Color.YELLOW);

        if(bufor!=null) {
            graph2d.drawImage(img, 0, 0, (int) (srcRect.width * magnification + 0.5), (int) (srcRect.height * magnification + 0.5),
                    srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, null);
        }
        startDrag = new Point(e.getX(), e.getY());
        endDrag = startDrag;
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        rectField = makeRectangle(startDrag.x,startDrag.y, e.getX(), e.getY());

        startDrag = null;
        endDrag = null;

        graph2d = (Graphics2D) getGraphics().create();
        graph2d.setColor(Color.GREEN);
        graph2d.drawRect((int) rectField.getBounds().getX(),(int) rectField.getBounds().getY(), rectField.getBounds().width, rectField.getBounds().height);

        System.out.println("Rectangle in X: " + (int) rectField.getBounds().getX() + " Y: " + (int) rectField.getBounds().getY() +
                " width: " + rectField.getBounds().width + " height: " + rectField.getBounds().height);

        Main.jInfo.setText("Rectangle in X: " + (int) rectField.getBounds().getX() + " Y: " + (int) rectField.getBounds().getY() +
                "\t width: " + rectField.getBounds().width + " height: " + rectField.getBounds().height);

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    @Override
    public void componentResized(ComponentEvent e) {

         bufor = new BufferedImage(this.getWidth(), this.getHeight(), imp.getType());

        graph2d = bufor.createGraphics();

        graph2d.setPaint(Color.white);
        graph2d.fillRect(0, 0, this.getWidth(), this.getHeight());

        if(bufor != null)
            graph2d.drawImage(bufor,0,0, bufor.getWidth(),bufor.getHeight(), this);

        img = bufor;
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        paint(graph2d);
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        endDrag = new Point(e.getX(), e.getY());
        paint(graph2d);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }


   public void paint(Graphics g) {

       super.paint(g);
       if (img!=null && bufor == null) {
           try {
               g.drawImage( img, 0, 0, (int) (srcRect.width * magnification + 0.5), (int) (srcRect.height * magnification + 0.5),
                       srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, this);

           } catch (Exception e) {
               System.out.println(e.getMessage());
           }

       }

        if (startDrag != null && endDrag != null) {
            rectField = makeRectangle(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
            getGraphics().drawRect((int) rectField.getBounds().getX(),(int) rectField.getBounds().getY(), rectField.getBounds().width, rectField.getBounds().height);
            graph2d = (Graphics2D) getGraphics().create();
        }
  }

    private Rectangle2D.Float makeRectangle(int x1, int y1, int x2, int y2) {
        return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public Rectangle2D getRectangle()
    {
        return rectField;
    }
}
