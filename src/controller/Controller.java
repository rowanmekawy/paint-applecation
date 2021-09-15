package controller;

import Paint.Shape;
import java.awt.BasicStroke;
import model.shapes.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.operations.MyDrawingEngine;
import model.operations.ShapeFactory;
import view.GUI;

public class Controller {

    private MyDrawingEngine engine;
    private ShapeFactory factory;
    private GUI gui;

    private String Mode = new String();

    private Shape newShape;
    private String newShapeType = new String();

    private ArrayList<Shape> AllShapes = new ArrayList<>();

    private Shape selectedShape;
    private Shape resizedShape;
    private Shape movedShape;
    private Shape copiedShape;
    private Shape updatedShape;

    private boolean copyFlag = false;

    java.awt.Rectangle highlightRect = new java.awt.Rectangle();

    private Color color = Color.BLACK;
    private Color FillColor = Color.WHITE;
    private float stroke = 3.0f;

    private Point endDrag;

    private int xStart;
    private int yStart;

    private int xEnd;
    private int yEnd;

    private int x1;
    private int y1;

    private int x2;
    private int y2;

    private int x3;
    private int y3;

    final private int PROX_DIST = 3;

    private Graphics2D g2;

    public Controller(MyDrawingEngine engine, ShapeFactory factory, GUI gui) {
        this.engine = engine;
        this.factory = factory;
        this.gui = gui;
        manageUndoAndRedo();

        g2 = this.gui.getJPanelGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        this.gui.addShapesListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    JList source = (JList) e.getSource();
                    if (source != null && source.getSelectedIndex() != -1) {
                        selectedShape = engine.getShapes()[source.getSelectedIndex()];
                        drawHighlightingRectangle(selectedShape);
                        paint();
                        /*
                        if (selectedShape != null) {
                            // namesList.removeListSelectionListener( this );
                            shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);

                            namesList.updateShapeNameList(engine.getShapes());
                            shapePropertiesPanel.addPositionSetterButtonListener(new positionSetterButtonListner());
                            shapePropertiesPanel.addPropSetterButtonListeners(new probSetterButtonListner());
                            // namesList.addListSelectionListener( this );
                        }
                         */
                    }
                }
            }
        });

        this.gui.addSaveXmlButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int retrival = chooser.showSaveDialog(null);
                if (retrival == JFileChooser.APPROVE_OPTION) {
                    Path path = Paths.get(chooser.getCurrentDirectory() + "/" + chooser.getSelectedFile().getName());
                    if (path != null) {
                        engine.save(path.toString().concat(".XmL"));
                    }
                }
            }
        });

        this.gui.addSaveJsonButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int retrival = chooser.showSaveDialog(null);
                if (retrival == JFileChooser.APPROVE_OPTION) {
                    Path path = Paths.get(chooser.getCurrentDirectory() + "/" + chooser.getSelectedFile().getName());
                    if (path != null) {
                        engine.save(path.toString().concat(".JsOn"));
                    }

                }
            }
        });
        
        this.gui.addLoadButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = File.separator + "tmp";
                JFileChooser fc = new JFileChooser(new File(filename));
                int result = fc.showOpenDialog(null);
                String selectedFilePath = fc.getSelectedFile().getPath().toString();
                if (result == JFileChooser.APPROVE_OPTION) {
                    engine.load(selectedFilePath);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = null;
                    paint();
                }
            }
        });
        
        this.gui.addSnapshotButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedShape = null;
                paint();
                BufferedImage image = new BufferedImage(gui.getJpanelWidth(), gui.getJpanelHeight(),
                        BufferedImage.TYPE_INT_RGB);
                paint(image.getGraphics());
                Graphics2D g = image.createGraphics();
                g.setBackground(Color.WHITE);
                g.clearRect(0, 0, gui.getJpanelWidth(), gui.getJpanelHeight());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                paint(g);
                JFileChooser chooser = new JFileChooser("");
                int retrival = chooser.showSaveDialog(null);
                if (retrival == JFileChooser.APPROVE_OPTION) {
                    Path path = Paths.get(chooser.getCurrentDirectory() + "/" + chooser.getSelectedFile().getName());
                    try {
                        ImageIO.write(image, "png", new File(path.toString().concat(".png")));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        
        
        this.gui.addPaleteButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setNullMode();
                if (gui.isFillColorButtonSelected()) {
                FillColor = JColorChooser.showDialog(null, "Choose a Color", FillColor);

                gui.setFillColorButtonBackground(FillColor);;
                if (selectedShape != null) {
                    try {
                        updatedShape = (Shape) selectedShape.clone();
                        updatedShape.setFillColor(FillColor);
                    } catch (CloneNotSupportedException e1) {
                        e1.printStackTrace();
                    }
                    engine.updateShape(selectedShape, updatedShape);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = updatedShape;
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                    paint();
                }
            } else if (gui.isColorButtonSelected()) {
                color = JColorChooser.showDialog(null, "Choose a Color", color);
                gui.setColorButtonBackground(color);
                if (selectedShape != null) {
                    try {
                        updatedShape = (Shape) selectedShape.clone();
                        updatedShape.setColor(color);
                    } catch (CloneNotSupportedException e1) {
                        e1.printStackTrace();
                    }
                    engine.updateShape(selectedShape, updatedShape);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = updatedShape;
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                    paint();
                }
            }
                gui.setColorModeNull();
            }
        });

        this.gui.addUndoButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(gui.isUndoEnabled()){
                    engine.undo();
                    selectedShape = engine.getRedoActions().get(engine.getRedoActions().size() - 1).getOldShape();
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    paint();
                }
            }
        });

        this.gui.addRedoButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(gui.isRedoEnabled()){
                    engine.redo();
                    selectedShape = engine.getUndoActions().get(engine.getUndoActions().size() - 1).getNewShape();
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    paint();
                }
            }
        });

        this.gui.addCreationButtonsActionListeners(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCreationMode();
                JToggleButton button = (JToggleButton) e.getSource();
                setNewShapeType(button.getToolTipText());

                selectedShape = null;
            }
        });
        
        this.gui.addExitButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.dispose();
                System.exit(0);
            }
        });

        this.gui.addResizeButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setResizeMode();

            }
        });

        this.gui.addMoveButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMotionMode();
            }
        });

        this.gui.addCopyButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMotionMode();
                copyFlag = true;
            }
        });

        this.gui.addPasteButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (copyFlag) {
                    try {
                        copiedShape = (Shape) selectedShape.clone();
                    } catch (CloneNotSupportedException e1) {
                        e1.printStackTrace();
                    }
                    Point point = copiedShape.getPosition();
                    if (copiedShape instanceof LineSegment) {
                        copiedShape.setPosition(new Point(point.x + 20, point.y));
                        Map<String, Double> prop = copiedShape.getProperties();
                        prop.put("x1", prop.get("x1") + 20);
                        prop.put("y1", prop.get("y1"));
                        prop.put("x2", prop.get("x2") + 20);
                        prop.put("y2", prop.get("y2"));
                        copiedShape.setProperties(prop);

                    } else if (copiedShape instanceof Triangle) {
                        copiedShape.setPosition(new Point(point.x + 20, point.y));
                        Map<String, Double> prop = copiedShape.getProperties();
                        prop.put("x1", prop.get("x1") + 20);
                        prop.put("y1", prop.get("y1"));
                        prop.put("x2", prop.get("x2") + 20);
                        prop.put("y2", prop.get("y2"));
                        prop.put("x3", prop.get("x3") + 20);
                        prop.put("y3", prop.get("y3"));
                        copiedShape.setProperties(prop);
                    } else {
                        copiedShape.setPosition(new Point(point.x + 20, point.y));
                    }

                    engine.addShape(copiedShape);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = copiedShape;
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    paint();
                    copyFlag = false;
                }
            }
        });
        
        this.gui.addDeleteButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(selectedShape != null){
                    engine.removeShape(selectedShape);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = null;
                    paint();
                }
            }
        });

        this.gui.addStrokeButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (stroke == 3.0f) {
                    stroke = 6.0f;
                } else if (stroke == 6.0f) {
                    stroke = 9.0f;
                } else {
                    stroke = 3.0f;
                }
                if (selectedShape != null) {
                    try {
                        updatedShape = (Shape) selectedShape.clone();
                    } catch (CloneNotSupportedException e1) {
                        e1.printStackTrace();
                    }
                    Map<String, Double> properties = updatedShape.getProperties();
                    properties.put("stroke", (double) stroke);
                    engine.updateShape(selectedShape, updatedShape);
                    updateShapes(engine.getShapes());
                    gui.updateShapesListModel(AllShapes);
                    selectedShape = updatedShape;
                    gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                    paint();
                    //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                }
            }
        });

        this.gui.addJPanelMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                if (isCreationMode()) {
                    selectedShape = null;
                    gui.updateShapesListModel(AllShapes);
                    createNewShape();
                    xStart = e.getX();
                    yStart = e.getY();
                    xEnd = e.getX();
                    yEnd = e.getY();
                    paint();
                } else if (isResizeMode() && selectedShape != null && gui.getCursor() != Cursor.getDefaultCursor()) {
                    try {
                        resizedShape = (Shape) selectedShape.clone();
                    } catch (CloneNotSupportedException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (isMotionMode() && selectedShape != null) {
                    try {
                        movedShape = (Shape) selectedShape.clone();
                    } catch (CloneNotSupportedException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    if(selectedShape!=null){
                        selectedShape = null;
                        gui.updateShapesListModel(AllShapes);
                        paint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                if (isCreationMode() && xStart!=e.getX() && yStart!=e.getY()) {
                    if (newShape instanceof RightAngledTriangle) {
                        xEnd = e.getX();
                        yEnd = e.getY();

                        x1 = xStart;
                        y1 = yStart;

                        x2 = xStart;
                        y2 = yEnd;

                        x3 = xEnd;
                        y3 = yEnd;

                    } else if (newShape instanceof Triangle) {
                        xEnd = e.getX();
                        yEnd = e.getY();

                        x1 = xStart;
                        y1 = yStart;

                        x2 = xEnd - 2 * (xEnd - xStart);
                        y2 = yEnd;

                        x3 = xEnd;
                        y3 = yEnd;

                    } else {
                        xEnd = e.getX();
                        yEnd = e.getY();
                    }

                    paint();
                    engine.addShape(newShape);
                    AllShapes.add(newShape);
                    newShape = null;
                    manageUndoAndRedo();
                    gui.updateShapesListModel(AllShapes);
                } else if (isResizeMode() && resizedShape != null) {
                    engine.updateShape(selectedShape, resizedShape);
                    selectedShape = resizedShape;
                    updateShapes(engine.getShapes());
                    
                    //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                    resizedShape = null;
                    paint();
                } else if (isMotionMode() && movedShape != null) {
                    if (!copyFlag) {
                        engine.updateShape(selectedShape, movedShape);
                        selectedShape = movedShape;
                        updateShapes(engine.getShapes());
                        gui.updateShapesListModel(AllShapes);
                        gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                        //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                        movedShape = null;
                        paint();
                    } else {
                        Point point = e.getPoint();
                        if (movedShape instanceof LineSegment) {
                            movedShape.setPosition(new Point(point.x, point.y));
                            Map<String, Double> prop = movedShape.getProperties();
                            prop.put("x1", prop.get("x1") + (point.x - prop.get("x2")));
                            prop.put("y1", prop.get("y1") + (point.y - prop.get("y2")));
                            prop.put("x2", (double) movedShape.getPosition().x);
                            prop.put("y2", (double) movedShape.getPosition().y);
                            movedShape.setProperties(prop);
                        } else if (movedShape instanceof Triangle) {
                            movedShape.setPosition(new Point(point.x, point.y));
                            Map<String, Double> prop = movedShape.getProperties();
                            prop.put("x2", prop.get("x2") + (point.x - prop.get("x1")));
                            prop.put("y2", prop.get("y2") + (point.y - prop.get("y1")));
                            prop.put("x3", prop.get("x3") + (point.x - prop.get("x1")));
                            prop.put("y3", prop.get("y3") + (point.y - prop.get("y1")));
                            prop.put("x1", movedShape.getPosition().getX());
                            prop.put("y1", movedShape.getPosition().getY());
                            movedShape.setProperties(prop);

                        } else {
                            movedShape.setPosition(new Point(point.x, point.y));
                        }

                        engine.addShape(movedShape);
                        updateShapes(engine.getShapes());
                        gui.updateShapesListModel(AllShapes);
                        selectedShape = movedShape;
                        gui.setShapeListSelectedIndex(AllShapes.indexOf(selectedShape));
                        //shapePropertiesPanel.updateShapePropertiesPanel(selectedShape);
                        movedShape = null;
                        copyFlag = false;
                        paint();
                    }
                }
            }

        });

        this.gui.addJPanelMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                gui.setMouseXLabel(String.valueOf(e.getX()));
                gui.setMouseYLabel(String.valueOf(e.getY()));
                if (isCreationMode()) {
                    if (newShape instanceof RightAngledTriangle) {
                        xEnd = e.getX();
                        yEnd = e.getY();

                        x1 = xStart;
                        y1 = yStart;

                        x2 = xStart;
                        y2 = yEnd;

                        x3 = xEnd;
                        y3 = yEnd;

                    } else if (newShape instanceof Triangle) {
                        xEnd = e.getX();
                        yEnd = e.getY();
                        x1 = xStart;
                        y1 = yStart;

                        x2 = xEnd - 2 * (xEnd - xStart);
                        y2 = yEnd;

                        x3 = xEnd;
                        y3 = yEnd;

                    } else {
                        xEnd = e.getX();
                        yEnd = e.getY();
                    }

                    paint();
                } else if (isResizeMode() && resizedShape != null) {
                    resize(resizedShape, e.getPoint());
                    paint();
                } else if (isMotionMode() && movedShape != null) {
                    endDrag = e.getPoint();
                    move(movedShape);
                    paint();
                }

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                gui.setMouseXLabel(String.valueOf(e.getX()));
                gui.setMouseYLabel(String.valueOf(e.getY()));

                if (isResizeMode() && selectedShape != null) {
                    Point p = e.getPoint();
                    if (!isOverRect(p)) {
                        if (gui.getCursor() != Cursor.getDefaultCursor()) {
                            // If cursor is not over rect reset it to the
                            // default.
                            gui.setCursor(Cursor.getDefaultCursor());
                        }
                        return;
                    }
                    // Locate cursor relative to center of rect.
                    int outcode = getOutcode(p);
                    java.awt.Rectangle r = highlightRect;
                    switch (outcode) {
                        case java.awt.Rectangle.OUT_TOP:
                            if (Math.abs(p.y - r.y) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_TOP + java.awt.Rectangle.OUT_LEFT:
                            if (Math.abs(p.y - r.y) < PROX_DIST && Math.abs(p.x - r.x) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_LEFT:
                            if (Math.abs(p.x - r.x) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_LEFT + java.awt.Rectangle.OUT_BOTTOM:
                            if (Math.abs(p.x - r.x) < PROX_DIST && Math.abs(p.y - (r.y + r.height)) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_BOTTOM:
                            if (Math.abs(p.y - (r.y + r.height)) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_BOTTOM + java.awt.Rectangle.OUT_RIGHT:
                            if (Math.abs(p.x - (r.x + r.width)) < PROX_DIST
                                    && Math.abs(p.y - (r.y + r.height)) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_RIGHT:
                            if (Math.abs(p.x - (r.x + r.width)) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                            }
                            break;
                        case java.awt.Rectangle.OUT_RIGHT + java.awt.Rectangle.OUT_TOP:
                            if (Math.abs(p.x - (r.x + r.width)) < PROX_DIST && Math.abs(p.y - r.y) < PROX_DIST) {
                                gui.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                            }
                            break;
                        default: // center
                            gui.setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        });
    }

    public void paint() {
        gui.updateJPanel(g2);
        engine.refresh(g2);
        manageUndoAndRedo();

        if (newShape != null) {
            g2.setPaint(Color.LIGHT_GRAY);
            setNewShapeProperties();
            newShape.draw(g2);
        }
        if (selectedShape != null) {
            drawHighlightingRectangle(selectedShape);
        }
        if (movedShape != null) {
            movedShape.draw(g2);
        }
        if (resizedShape != null) {
            resizedShape.draw(g2);
        }
        
    }
    
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        gui.updateJPanel(g2);
        engine.refresh(g2);
        manageUndoAndRedo();

        if (newShape != null) {
            g2.setPaint(Color.LIGHT_GRAY);
            setNewShapeProperties();
            newShape.draw(g2);
        }
        if (movedShape != null) {
            movedShape.draw(g2);
        }
        if (resizedShape != null) {
            resizedShape.draw(g2);
        }
        if (selectedShape != null) {
            drawHighlightingRectangle(selectedShape);
        }
                
    }

    
    public void manageUndoAndRedo() {
        if (engine.getUndoActions().isEmpty()) {
            gui.disableUndo();
        } else {
            gui.enableUndo();
        }
        if (engine.getRedoActions().isEmpty()) {
            gui.disableRedo();
        } else {
            gui.enableRedo();
        }
    }

    public void setCreationMode() {
        this.Mode = "Creation";
    }

    public void setMotionMode() {
        this.Mode = "Motion";
    }

    public void setResizeMode() {
        this.Mode = "Resize";
    }

    public void setNullMode() {
        this.Mode = "null";
    }

    public boolean isCreationMode() {
        return (this.Mode.equals("Creation"));
    }

    public boolean isMotionMode() {
        return (this.Mode.equals("Motion"));
    }

    public boolean isResizeMode() {
        return (this.Mode.equals("Resize"));
    }

    public boolean isNullMode() {
        return (this.Mode.equals("null"));
    }

    public void setNewShapeType(String type) {
        newShapeType = type;
    }

    public void createNewShape() {
        newShape = factory.createShape(newShapeType);
    }

    public void setNewShapeProperties() {
        newShape.setColor(color);
        newShape.setFillColor(FillColor);
        newShape.setPosition(new Point(xStart, yStart));
        Map<String, Double> properties = newShape.getProperties();
        properties.put("stroke", (double) stroke);

        if (newShape instanceof LineSegment) {
            properties.put("x1", (double) xStart);
            properties.put("y1", (double) yStart);
            properties.put("x2", (double) xEnd);
            properties.put("y2", (double) yEnd);
        } else if (newShape instanceof Square) {
            properties.put("xAxis", (double) xEnd - xStart);
            properties.put("yAxis", (double) xEnd - xStart);

        } else if (newShape instanceof Rectangle) {
            properties.put("xAxis", (double) xEnd - xStart);
            properties.put("yAxis", (double) yEnd - yStart);

        } else if (newShape instanceof Circle) {
            properties.put("xAxis", (double) xEnd - xStart);
            properties.put("yAxis", (double) xEnd - xStart);

        } else if (newShape instanceof Ellipse) {
            properties.put("xAxis", (double) xEnd - xStart);
            properties.put("yAxis", (double) yEnd - yStart);

        } else if (newShape instanceof Triangle) {
            properties.put("x1", (double) x1);
            properties.put("y1", (double) y1);
            properties.put("x2", (double) x2);
            properties.put("y2", (double) y2);
            properties.put("x3", (double) x3);
            properties.put("y3", (double) y3);
        }

        newShape.setProperties(properties);
    }

    public void updateShapes(Shape[] AllShapes) {
        this.AllShapes.clear();
        for (Shape i : AllShapes) {
            this.AllShapes.add(i);
        }
    }

    public void reDrawAllShapes() {
        for (Shape i : AllShapes) {
            i.draw(g2);
        }
    }

    public void drawHighlightingRectangle(Shape selectedShape) {
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{10f, 10f}, 0));
        g2.setPaint(Color.BLACK);

        if (selectedShape instanceof Ellipse || selectedShape instanceof Rectangle) {
            Map<String, Double> properties = selectedShape.getProperties();
            int stroke = properties.get("stroke").intValue();
            Point position = selectedShape.getPosition();
            int width = properties.get("xAxis").intValue();
            int height = properties.get("yAxis").intValue();

            highlightRect.x = position.x - (2 + stroke);
            highlightRect.y = position.y - (2 + stroke);
            highlightRect.width = width + (4 + 2 * stroke);
            highlightRect.height = height + (4 + 2 * stroke);
            g2.drawRect(highlightRect.x, highlightRect.y, highlightRect.width, highlightRect.height);
        } else if (selectedShape instanceof LineSegment) {
            Map<String, Double> properties = selectedShape.getProperties();
            int minX;
            int minY;
            int maxX;
            int maxY;
            int x1 = properties.get("x1").intValue();
            minX = x1;
            maxX = x1;
            int y1 = properties.get("y1").intValue();
            minY = y1;
            maxY = y1;
            int x2 = properties.get("x2").intValue();
            if (maxX < x2) {
                maxX = x2;
            }
            if (minX > x2) {
                minX = x2;
            }
            int y2 = properties.get("y2").intValue();
            if (maxY < y2) {
                maxY = y2;
            }
            if (minY > y2) {
                minY = y2;
            }
            if (maxX - minX == 0) {
                highlightRect.x = minX - 5;
                highlightRect.y = minY;
                highlightRect.width = 10;
                highlightRect.height = maxY - minY;
                g2.drawRect(highlightRect.x, highlightRect.y, highlightRect.width, highlightRect.height);
            } else if (maxY - minY == 0) {
                highlightRect.x = minX;
                highlightRect.y = minY - 5;
                highlightRect.width = maxX - minX;
                highlightRect.height = 10;
                g2.drawRect(highlightRect.x, highlightRect.y, highlightRect.width, highlightRect.height);
            } else {
                highlightRect.x = minX - 5;
                highlightRect.y = minY - 5;
                highlightRect.width = (maxX - minX) + 10;
                highlightRect.height = (maxY - minY) + 10;
                g2.drawRect(highlightRect.x, highlightRect.y, highlightRect.width, highlightRect.height);
            }
        } else if (selectedShape instanceof Triangle) {
            Map<String, Double> properties = selectedShape.getProperties();
            int x1 = properties.get("x1").intValue();
            int y1 = properties.get("y1").intValue();
            int x2 = properties.get("x2").intValue();
            int y2 = properties.get("y2").intValue();
            int x3 = properties.get("x3").intValue();
            int y3 = properties.get("y3").intValue();

            int stroke = properties.get("stroke").intValue();

            highlightRect.x = x2 - (2 + stroke);
            highlightRect.y = y1 - (2 + stroke);
            highlightRect.width = (x3 - x2) + (4 + 2 * stroke);
            highlightRect.height = (y2 - y1) + (4 + 2 * stroke);
            g2.drawRect(highlightRect.x, highlightRect.y, highlightRect.width, highlightRect.height);

        }

    }

    private int getOutcode(Point p) {
        java.awt.Rectangle r = (java.awt.Rectangle) this.highlightRect.clone();
        r.grow(-PROX_DIST, -PROX_DIST);
        return r.outcode(p.x, p.y);
    }

    private boolean isOverRect(Point p) {
        java.awt.Rectangle r = (java.awt.Rectangle) this.highlightRect.clone();
        r.grow(PROX_DIST, PROX_DIST);
        return r.contains(p);
    }

    public void resize(Shape shape, Point p) {
        int type = gui.getCursor().getType();
        if (shape instanceof Square) {
            Map<String, Double> RectMap = shape.getProperties();
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Double width = RectMap.get("xAxis"); // = y1-y2
            Double height = RectMap.get("yAxis"); // = x2-x1
            Double dx = p.x - x;
            Double dy = p.y - y;
            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    RectMap.put("xAxis", width - dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() +dy), (int) (y.intValue() + dy)));
                    RectMap.put("xAxis", height - dy);
                    RectMap.put("yAxis", height - dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("xAxis", height - dx);
                    RectMap.put("yAxis", height - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("xAxis", height - dx);
                    RectMap.put("yAxis", height - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    RectMap.put("yAxis", dx);
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    RectMap.put("xAxis", width - dy);
                    shape.setProperties(RectMap);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        }
        else if (shape instanceof Rectangle) {
            Map<String, Double> RectMap = shape.getProperties();
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Double width = RectMap.get("xAxis"); // = y1-y2
            Double height = RectMap.get("yAxis"); // = x2-x1
            Double dx = p.x - x;
            Double dy = p.y - y;
            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), (int) (y.intValue() + dy)));
                    RectMap.put("xAxis", width - dx);
                    RectMap.put("yAxis", height - dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("xAxis", width - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", width - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        }
        else if (shape instanceof Circle) {
            Map<String, Double> properties = shape.getProperties();
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Double width = properties.get("xAxis"); // = y1-y2
            Double height = properties.get("yAxis"); // = x2-x1
            Double dx = p.x - x;
            Double dy = p.y - y;
            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    properties.put("yAxis", height - dy);
                    properties.put("xAxis", width - dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() +dy), (int) (y.intValue() + dy)));
                    properties.put("xAxis", height - dy);
                    properties.put("yAxis", height - dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    properties.put("xAxis", height - dx);
                    properties.put("yAxis", height - dx);
                    shape.setProperties(properties);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    properties.put("xAxis", height - dx);
                    properties.put("yAxis", height - dx);
                    shape.setProperties(properties);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    properties.put("yAxis", dy);
                    properties.put("xAxis", dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    properties.put("yAxis", dy);
                    properties.put("xAxis", dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    properties.put("yAxis", dx);
                    properties.put("xAxis", dx);
                    shape.setProperties(properties);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    properties.put("yAxis", height - dy);
                    properties.put("xAxis", width - dy);
                    shape.setProperties(properties);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        }
        else if (shape instanceof Ellipse) {
            Map<String, Double> RectMap = shape.getProperties();
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Double width = RectMap.get("xAxis");
            Double height = RectMap.get("yAxis");
            Double dx = p.x - x;
            Double dy = p.y - y;
            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    RectMap.put("xAxis", width - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("xAxis", width - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", width - dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    RectMap.put("yAxis", dy);
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    RectMap.put("yAxis", height - dy);
                    RectMap.put("xAxis", dx);
                    shape.setProperties(RectMap);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        } else if (shape instanceof RightAngledTriangle) {
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Map<String, Double> properties = shape.getProperties();
            Double x1 = properties.get("x1");
            Double y1 = properties.get("y1");
            Double x2 = properties.get("x2");
            Double y2 = properties.get("y2");
            Double x3 = properties.get("x3");
            Double y3 = properties.get("y3");
            Double dx = p.x - x;
            Double dy = p.y - y;

            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    properties.put("y1", y1 + dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), (int) (y.intValue() + dy)));
                    properties.put("y1", y1 + dy);
                    properties.put("x1", x1 + dx);
                    properties.put("x2", x2 + dx);
                    shape.setProperties(properties);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    properties.put("x1", x1 + dx);
                    properties.put("x2", x2 + dx);
                    shape.setProperties(properties);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    properties.put("x1", x1 + dx);
                    properties.put("x2", x2 + dx);
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    shape.setProperties(properties);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    shape.setProperties(properties);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    properties.put("x3", dx + x);
                    shape.setProperties(properties);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    properties.put("x3", dx + x);
                    shape.setProperties(properties);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    properties.put("y1", y1 + dy);
                    properties.put("x3", dx + x);
                    shape.setProperties(properties);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        } else if (shape instanceof Triangle) {
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();
            Map<String, Double> properties = shape.getProperties();
            Double x1 = properties.get("x1");
            Double y1 = properties.get("y1");
            Double x2 = properties.get("x2");
            Double y2 = properties.get("y2");
            Double x3 = properties.get("x3");
            Double y3 = properties.get("y3");
            Double dx = p.x - x;
            Double dy = p.y - y;

            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    shape.setPosition(new Point(x.intValue(), (int) (y.intValue() + dy)));
                    properties.put("y1", y1 + dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), (int) (y.intValue() + dy)));
                    x2 = dx + x;
                    x3 = 2 * x1 - x2;
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    properties.put("y1", y1 + dy);
                    shape.setProperties(properties);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    x2 = dx + x;
                    x3 = 2 * x1 - x2;
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    shape.setProperties(properties);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() + dx), y.intValue()));
                    x2 = dx + x;
                    x3 = 2 * x1 - x2;
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    shape.setProperties(properties);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    shape.setProperties(properties);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() - dx), y.intValue()));
                    x3 = dx + x;
                    x2 = x3 - 2 * (x3 - x1);
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    properties.put("y2", dy + y);
                    properties.put("y3", dy + y);
                    shape.setProperties(properties);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() - dx), y.intValue()));
                    x3 = dx + x;
                    x2 = x3 - 2 * (x3 - x1);
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    shape.setProperties(properties);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    shape.setPosition(new Point((int) (x.intValue() - dx), (int) (y.intValue() + dy)));
                    x3 = dx + x;
                    x2 = x3 - 2 * (x3 - x1);
                    properties.put("x2", x2);
                    properties.put("x3", x3);
                    properties.put("y1", y1 + dy);
                    shape.setProperties(properties);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }
        } else if (shape instanceof LineSegment) {

            Map<String, Double> properties = shape.getProperties();
            Double x = shape.getPosition().getX();
            Double y = shape.getPosition().getY();

            Double x1 = properties.get("x1");
            Double y1 = properties.get("y1");
            Double x2 = properties.get("x2");
            Double y2 = properties.get("y2");

            Double dx = p.getX() - x;
            Double dy = p.getY() - y;

            switch (type) {
                case Cursor.N_RESIZE_CURSOR:
                    if (y1 < y2) {
                        properties.put("y1", y1 + dy);
                    } else if (y1 > y2) {
                        properties.put("y2", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    if (y1 < y2 && x1 < x2) {
                        properties.put("x1", x1 + dx);
                        properties.put("y1", y1 + dy);
                    } else if (y1 < y2 && x1 > x2) {
                        properties.put("x2", dx + x);
                        properties.put("y1", y1 + dy);
                    } else if (y1 > y2 && x1 < x2) {
                        properties.put("x1", x1 + dx);
                        properties.put("y2", dy + y);
                    } else if (y1 > y2 && x1 > x2) {
                        properties.put("x2", dx + x);
                        properties.put("y2", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    if (x1 < x2) {
                        properties.put("x1", x1 + dx);
                    } else if (x1 > x2) {
                        properties.put("x2", dx + x);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    if (y1 < y2 && x1 < x2) {
                        properties.put("x1", x1 + dx);
                        properties.put("y2", dy + y);
                    } else if (y1 < y2 && x1 > x2) {
                        properties.put("x2", dx + x);
                        properties.put("y2", dy + y);
                    } else if (y1 > y2 && x1 < x2) {
                        properties.put("x1", x1 + dx);
                        properties.put("y1", dy + y);
                    } else if (y1 > y2 && x1 > x2) {
                        properties.put("x2", dx + x);
                        properties.put("y1", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    if (y1 < y2) {
                        properties.put("y2", dy + y);
                    } else if (y1 > y2) {
                        properties.put("y1", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    if (y1 < y2 && x1 < x2) {
                        properties.put("x2", dx + x);
                        properties.put("y2", dy + y);
                    } else if (y1 < y2 && x1 > x2) {
                        properties.put("x1", dx + x);
                        properties.put("y2", dy + y);
                    } else if (y1 > y2 && x1 < x2) {
                        properties.put("x2", dx + x);
                        properties.put("y1", dy + y);
                    } else if (y1 > y2 && x1 > x2) {
                        properties.put("x1", dx + x);
                        properties.put("y1", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    if (x1 < x2) {
                        properties.put("x2", dx + x);
                    } else if (x1 > x2) {
                        properties.put("x1", dx + x);
                    }
                    shape.setProperties(properties);
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    if (y1 < y2 && x1 < x2) {
                        properties.put("x2", dx + x);
                        properties.put("y1", y1 + dy);
                    } else if (y1 < y2 && x1 > x2) {
                        properties.put("x1", dx + x);
                        properties.put("y1", y1 + dy);
                    } else if (y1 > y2 && x1 < x2) {
                        properties.put("x2", dx + x);
                        properties.put("y2", dy + y);
                    } else if (y1 > y2 && x1 > x2) {
                        properties.put("x1", dx + x);
                        properties.put("y2", dy + y);
                    }
                    shape.setProperties(properties);
                    break;
                default:
                    System.out.println("unexpected type: " + type);

            }

        }
    }

    public void move(Shape shape) {
        Point prePoint = shape.getPosition();
        shape.setPosition(endDrag);
        Map<String, Double> properties = shape.getProperties();

        if (shape instanceof LineSegment) {
            Double x2 = ((properties.get("x2") + (endDrag.x - prePoint.x)));
            int y2 = (int) (properties.get("y2") + (endDrag.y - prePoint.y));
            properties.put("x2", x2);
            properties.put("y2", (double) y2);
            properties.put("x1", shape.getPosition().getX());
            properties.put("y1", shape.getPosition().getY());
            shape.setProperties(properties);
        }
        if (shape instanceof Triangle) {
            Double x2 = ((properties.get("x2") + (endDrag.x - prePoint.x)));
            int y2 = (int) (properties.get("y2") + (endDrag.y - prePoint.y));
            Double x3 = ((properties.get("x3") + (endDrag.x - prePoint.x)));
            int y3 = (int) (properties.get("y3") + (endDrag.y - prePoint.y));
            properties.put("x2", x2);
            properties.put("y2", (double) y2);
            properties.put("x3", x3);
            properties.put("y3", (double) y3);
            properties.put("x1", shape.getPosition().getX());
            properties.put("y1", shape.getPosition().getY());
            shape.setProperties(properties);
        }
    }

}
