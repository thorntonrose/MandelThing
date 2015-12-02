import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 * <p>This is a simple program to plot the Mandelbrot set. The general equation is</p>
 *
 * <p> z = z^2 + c </p>
 *
 * <p>where z & c are complex numbers (a*i + r). The real and imaginary parts
 * are expressed as </p>
 *
 * <p> zr(n+1) = zr^2 - zi^2 + cr   <br>
 *     zi = (2 * zr(n) * zi) + ci </p>
 *
 * <p>where Xr denotes a real part, and Xi denotes an imaginary part. The depth is
 * calculated by evaluating the above equations until</p>
 *
 * <p> (zr^2 + zi^2) > 4 </p>
 *
 * <p>or a maximum number of iterations (256 by default) has been reached. If the
 * depth is greater than the maximum number of iterations, it is considered to be
 * infinity, and the point is colored black. Otherwise, the color is determined by
 * using the depth as the index into the color map (a single dimension array of
 * colors).</p>
 *
 * <p>The initial bounds for the set are</p>
 *
 * <pre>               i
 *  (-2.5,1.5)+--------+-----+
 *            |        |     |
 *            |--------0-----| r
 *            |        |     |
 *            +--------+-----+(1.5,-1.5)
 * </pre>
 */
public class MandelThing extends JFrame {
   public static final String TITLE = "MandelThing";
   public static final String VERSION = "1.0";

   private int     defaultMaxDepth = 256;
   private int     defaultImageWidth = 640;
   private int     defaultImageHeight = 480;
   private int     maxDepth;
   private int     imageWidth;
   private int     imageHeight;
   private double  ar;            // Top-left, real part
   private double  ai;            // Top-left, imaginary part
   private double  br;            // Bottom-right, real part
   private double  bi;            // Bottom-right, imaginary part
   private int     zbLeft = -1;   // Left coordinate of zoom box
   private int     zbTop = -1;    // Top coordinate of zoom box
   private int     zbWidth = 0;   // Zoom box width
   private int     zbHeight = 0;  // Zoom box height
   private boolean zbOn = false;  // True if zoom box on
   private Color[] colorMap;
   private Image   imageBuffer;   // Image of current plot

   private BorderLayout borderLayout1 = new BorderLayout();
   private BorderLayout borderLayout2 = new BorderLayout();
   private JPanel imagePanel = new JPanel();
   private JPanel controlPanel = new JPanel();
   private JPanel buttonPanel1 = new JPanel();
   private JLabel maxDepthLabel = new JLabel();
   private JTextField maxDepthField = new JTextField();
   private JButton resetButton = new JButton();
   private GridLayout gridLayout1 = new GridLayout();
   private JPanel panel5 = new JPanel();
   private JButton plotButton = new JButton();
   private GridLayout gridLayout2 = new GridLayout();
   private JPanel buttonPanel2 = new JPanel();
   private JButton aboutButton = new JButton();
   private JPanel spacerPanel = new JPanel();

   //------------------------------------------------------------------------------------
   // Constructors
   //------------------------------------------------------------------------------------

   public MandelThing() {
      // Load the properties file.

      loadProperties();
      setDefaultParameters();

      // Initialize UI components.

      jbInit();
      pack();
      initColorMap();
      initGraphics();

      // Center on screen.

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation(
         (screenSize.width - getSize().width) / 2,
         ((screenSize.height - getSize().height) / 2) - 25);

      // Add window listener.

      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent event) {
            System.exit(0);
         }
      });

      // Add listener to plot button.

      plotButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            plot();
         }
      });

      // Add listener to reset button.

      resetButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            setDefaultParameters();
            plot();
         }
      });

      // Add mouse listeners to image panel to listen for mouse press and drag in
      // order to draw zoom box.

      imagePanel.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent event) {
            // System.out.println("mousePressed: " + event.getPoint());
            startZoomBox(event.getPoint());
         }
      });

      imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
         public void mouseDragged(MouseEvent event) {
            // System.out.println("mouseDragged: " + event.getPoint());
            dragZoomBox(event.getPoint());
         }
      });

      // Add listener to About button.

      aboutButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            showAbout();
         }
      });
   }

   //------------------------------------------------------------------------------------
   // JBuilder UI code
   //------------------------------------------------------------------------------------

   /**
    * Initialize the UI components.
    */
   private void jbInit() {
      this.setTitle("MandelThing");
      this.setResizable(false);
      this.setSize(new Dimension(640, 480));
      this.getContentPane().setLayout(borderLayout1);
      this.getContentPane().add(imagePanel, BorderLayout.CENTER);
      this.getContentPane().add(controlPanel, BorderLayout.SOUTH);

      imagePanel.setOpaque(true);
      imagePanel.setLayout(null);
      imagePanel.setBackground(Color.black);
      imagePanel.setMinimumSize(new Dimension(imageWidth, imageHeight));
      imagePanel.setMaximumSize(new Dimension(imageWidth, imageHeight));
      imagePanel.setPreferredSize(new Dimension(imageWidth, imageHeight));

      controlPanel.setMinimumSize(new Dimension(10, 25));
      controlPanel.setPreferredSize(new Dimension(10, 25));
      controlPanel.setLayout(borderLayout2);
      controlPanel.add(spacerPanel, BorderLayout.NORTH);
      controlPanel.add(buttonPanel1, BorderLayout.CENTER);
      controlPanel.add(buttonPanel2, BorderLayout.EAST);

      spacerPanel.setBorder(BorderFactory.createEtchedBorder());
      spacerPanel.setMinimumSize(new Dimension(10, 2));
      spacerPanel.setMaximumSize(new Dimension(10, 2));
      spacerPanel.setPreferredSize(new Dimension(10, 2));

      buttonPanel1.setLayout(null);
      buttonPanel1.setMinimumSize(new Dimension(280, 23));
      buttonPanel1.setPreferredSize(new Dimension(280, 23));
      buttonPanel1.add(maxDepthLabel, null);
      buttonPanel1.add(maxDepthField, null);
      buttonPanel1.add(panel5, null);
      panel5.add(plotButton, null);
      panel5.add(resetButton, null);

      maxDepthLabel.setText("Max. Depth:");
      maxDepthLabel.setBounds(new Rectangle(0, 1, 79, 21));

      maxDepthField.setText("256");
      maxDepthField.setBounds(new Rectangle(79, 1, 45, 21));

      gridLayout1.setColumns(2);
      gridLayout1.setHgap(2);

      panel5.setLayout(gridLayout1);
      panel5.setMinimumSize(new Dimension(124, 23));
      panel5.setPreferredSize(new Dimension(124, 23));
      panel5.setBounds(new Rectangle(133, 0, 124, 23));

      plotButton.setMargin(new Insets(0, 0, 0, 0));
      plotButton.setText("Plot");

      resetButton.setMargin(new Insets(0, 0, 0, 0));
      resetButton.setText("Reset");

      gridLayout2.setHgap(2);
      gridLayout2.setColumns(1);

      buttonPanel2.add(aboutButton, null);
      buttonPanel2.setPreferredSize(new Dimension(25, 23));
      buttonPanel2.setMinimumSize(new Dimension(25, 23));
      buttonPanel2.setLayout(gridLayout2);

      aboutButton.setMargin(new Insets(0, 0, 0, 0));
      aboutButton.setText("(i)");
      aboutButton.setToolTipText("About");
   }

   //------------------------------------------------------------------------------------
   // Plotting and image methods
   //------------------------------------------------------------------------------------

   /**
    * Initialize the color map.
    */
   private void initColorMap() {
      colorMap = new Color[256];

      for (int i = 0; i < 256; i ++) {
         int j = (i * 16) % 256;
         colorMap[i] = (new Color(0, 0, j)).brighter();
      }

      /*
      colorMap = new Color[256 * 7];

      for (int i = 0; i < 256; i ++) {
         int j = (i * 16) % 256;

         colorMap[i + (256 * 4)] = (new Color(j, 0, 0)).brighter(); // red
         colorMap[i + (256 * 1)] = (new Color(0, j, 0)).brighter(); // green
         colorMap[i + (256 * 2)] = (new Color(j, j, j)).brighter(); // gray
         colorMap[i + (256 * 5)] = (new Color(j, 0, j)).brighter(); // violet
         colorMap[i + (256 * 6)] = (new Color(j, j, 0)).brighter(); // yellow
         colorMap[i + (256 * 3)] = (new Color(0, j, j)).brighter(); // cyan
         colorMap[i + (256 * 1)] = (new Color(0, 0, j)).brighter(); // blue
      }
      */
   }

   /**
    * Create the image buffer if necessary and clear the graphics spaces.
    */
   private void initGraphics() {
      if (imageBuffer == null) {
         imageBuffer = createImage(imageWidth, imageHeight);
      }

      Graphics ig = imageBuffer.getGraphics();
      ig.setColor(Color.black);
      ig.fillRect(0, 0, imageWidth, imageHeight);
      drawImage();
   }

   /**
    * Plot the fractal.
    */
   public void plot() {
      double   cr = 0.0;   // calculated point, real part
      double   ci = 0.0;   // calculated point, imaginary part
      double   zr = 0.0;
      double   zi = 0.0;
      double   zr2 = 0.0;
      int      d = 0;      // depth of calculated point
      int      x;
      int      y;
      Color    cc = null;  // color of calculated point
      Graphics ig = imageBuffer.getGraphics();
      Graphics pg = imagePanel.getGraphics();

      System.out.println("Plotting...");

      // Check the parameters. If invalid, return.

      if (! getParameters()) {
         System.out.println("Invalid parameters.");
         return;
      }

      // Initialize the graphics spaces.

      // initColorMap();
      initGraphics();

      System.out.println("maxDepth    = " + maxDepth);
      System.out.println("imageWidth  = " + imageWidth);
      System.out.println("imageHeight = " + imageHeight);
      System.out.println("ar          = " + ar);
      System.out.println("ai          = " + ai);
      System.out.println("br          = " + br);
      System.out.println("bi          = " + bi);

      // Iterate through graphics area.

      for (y = 0; y < imageHeight; y ++) {
         // Fracman: tl.imag() + (double) y / h * (br.imag() - tl.imag())
         ci = imaginaryPart(y);

         for (x = 0; x < imageWidth; x ++) {
            // Fracman: tl.real() + (double) x / w * (br.real() - tl.real())
            cr = realPart(x);
            zr = 0.0;
            zi = 0.0;
            zr2 = 0.0;

            // Iterate the actual Mandelbrot equation to find the depth of the
            // point within the set.

            for (d = 0; d < maxDepth; d ++) {
               zr2 = ((zr * zr) - (zi * zi)) + cr;
               zi = (2.0 * zr * zi) + ci;
               zr = zr2;

               if (((zr * zr) + (zi * zi)) > 4.0) {
                  break;
               }
            }

            // Draw the pixel. If the depth was not infinity (greater than max.
            // depth), determine the color from the color map. Otherwise, use black.

            cc = (d < maxDepth ? colorMap[d % colorMap.length] : Color.black);
            plotPoint(pg, x, y, cc);
            plotPoint(ig, x, y, cc);
         }
      }

      repaint();
      System.out.println("Done.");
   }

   /**
    * Get the plot parameters. Return true if they are valid.
    */
   public boolean getParameters() {
      // Get maximum depth.

      try {
         maxDepth = Integer.parseInt(maxDepthField.getText().trim());
      } catch(NumberFormatException ex) {
         maxDepth = 0;
      }

      if (maxDepth < 2) {
         JOptionPane.showMessageDialog(
            this,
            "Depth must be >= 2.",
            "Invalid Depth",
            JOptionPane.INFORMATION_MESSAGE);
         return false;
      }

      // If zoom box is on, get bounds.

      if (zbOn) {
         ar = realPart(zbLeft);
         ai = imaginaryPart(zbTop);
         br = realPart(zbWidth);
         bi = imaginaryPart(zbHeight);
         zbOn = false;
      }

      return true;
   }

   /**
    * Plot a point at the given coordinates in the given color in the given graphics
    * space.
    */
   private void plotPoint(Graphics g, int x, int y, Color c) {
      g.setColor(c);
      g.drawLine(x, y, x, y);
   }

   /**
    * Overrides default update method. Reduces flicker by calling paint without
    * first clearing the screen.
    */
   public void update(Graphics g) {
      paint(g);
   }

   /**
    * Called when the screen needs to be repainted.
    */
   public void paint(Graphics g) {
      super.paint(g);
      drawImage();
   }

   /**
    * Draw the image buffer onto the image panel.
    */
   private void drawImage() {
      imagePanel.getGraphics().drawImage(imageBuffer, 0, 0, this);
   }

   //------------------------------------------------------------------------------------
   // Zoom box methods
   //------------------------------------------------------------------------------------

   private void startZoomBox(Point p) {
      // If the zoom box is on, draw over it so that it disappears, then set the
      // "on" flag to false. This seems counter-intuitive, but it lets a single
      // mouse click clear the zoom box and not turn it on unless a drag occurs.

      if (zbOn) {
         drawZoomBox();
         zbOn = false;
         System.out.println("Zoom box off.");
      }

      // Set the top, left, width, and height of the zoom box.

      zbLeft = p.x;
      zbTop = p.y;
      zbWidth = 0;
      zbHeight = 0;
   }

   private void dragZoomBox(Point p) {
      if (! zbOn) {
         zbOn = true;
         System.out.println("Zoom box on.");
      }

      drawZoomBox();
      zbWidth = Math.abs(p.x - zbLeft) + 1;
      zbHeight = Math.abs(p.y - zbTop) + 1;
      drawZoomBox();
   }

   private void drawZoomBox() {
      Graphics g = imagePanel.getGraphics();
      g.setColor(Color.black);
      g.setXORMode(Color.white);
      g.drawRect(zbLeft, zbTop, zbWidth, zbHeight);
   }

   //------------------------------------------------------------------------------------
   // Math functions
   //------------------------------------------------------------------------------------

   /**
    * Calculate real part of complex point given x in cartesian space.
    */
   private double realPart(int x) {
      return ar + (double) x / imageWidth * (br - ar);
   }

   /**
    * Calculate imaginary part of complex point given y in cartesian space.
    */
   private double imaginaryPart(int y) {
      return ai + (double) y / imageHeight * (bi - ai);
   }

   //------------------------------------------------------------------------------------
   // Utility methods
   //------------------------------------------------------------------------------------

   /**
    * Show application identification information.
    */
   private void showAbout() {
      JOptionPane.showMessageDialog(
         this,
         TITLE + " v" + VERSION + "\n\nDeveloped by Thornton Rose\nCopyright 2000",
         "About",
         JOptionPane.INFORMATION_MESSAGE);
   }

   /**
    * Load the default parameters from the properties file. If an error occurs while
    * loading the properties file, it is reported to the console.
    */
   private void loadProperties() {
      try {
         System.out.println("Loading properties...");
         FileInputStream propFile = new FileInputStream("MandelThing.properties");

         try {
            Properties props = new Properties();
            props.load(propFile);

            // Get default max. depth.

            try {
               defaultMaxDepth = Integer.parseInt(props.getProperty("maxdepth"));
            } catch(NumberFormatException ex) {
            }

            // Get default image width.

            try {
               defaultImageWidth = Integer.parseInt(props.getProperty("width"));
            } catch(NumberFormatException ex) {
            }

            // Get default image height.

            try {
               defaultImageHeight = Integer.parseInt(props.getProperty("height"));
            } catch(NumberFormatException ex) {
            }
         } finally {
            propFile.close();
         }

         System.out.println("Done.");
      } catch(Exception ex) {
         System.out.println(ex);
      }
   }

   /**
    * Set the default parameter values.
    */
   private void setDefaultParameters() {
      maxDepth = defaultMaxDepth;
      maxDepthField.setText(Integer.toString(maxDepth));

      imageWidth = defaultImageWidth;
      imageHeight = defaultImageHeight;

      ar = -2.5;
      ai = 1.5;
      br = 1.5;
      bi = -1.5;

      zbOn = false;
   }

   //------------------------------------------------------------------------------------
   // Main
   //------------------------------------------------------------------------------------

   /**
    * Start the app.
    */
   public static void main(String args[]) {
      try {
         MandelThing app = new MandelThing();
         app.show();
      } catch(Exception ex) {
         ex.printStackTrace();
         System.exit(0);
      }
   }
}