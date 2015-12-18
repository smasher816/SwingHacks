package com.smartg.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import com.smartg.java.util.EventListenerListIterator;

/**
 * JRangeSlider. This class implements slider with two values. Second value is
 * equals to first value plus extent, so I just reused BoundedRangeModel.
 * JRangeSlider will look correct on all platforms (using appropriate SliderUI).
 * 
 * @author andronix
 *
 */
public class JRangeSlider extends JPanel {

    // used to get access to protected goodies

    private final class RangeSliderUI extends BasicSliderUI {
	public RangeSliderUI(JSlider slider) {
	   super(slider);
	}
	Rectangle getThumbRect() {
	    calculateThumbLocation();
	    return new Rectangle(this.thumbRect);
	}
    }

    private final class MouseHandler extends MouseAdapter {
	private int cursorType;
	private int pressX, pressY;
	private int modelValue;
	private int modelExtent;

	@Override
	public void mouseMoved(MouseEvent e) {
	    int x = e.getX();
	    int y = e.getY();
	    boolean horizontal = (slider.getOrientation() == SwingConstants.HORIZONTAL);
	    if (extentThumbRect.contains(x,y)) {
		cursorType = horizontal ? Cursor.E_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR;
	    } else if (thumbRect.contains(x,y)) {
		cursorType = horizontal ? Cursor.W_RESIZE_CURSOR : Cursor.S_RESIZE_CURSOR;
	    } else if (middleRect.contains(x,y)) {
		cursorType = Cursor.MOVE_CURSOR;
	    } else {
		cursorType = Cursor.DEFAULT_CURSOR;
	    }
	    setCursor(Cursor.getPredefinedCursor(cursorType));
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	    float delta;
	    switch (cursorType) {
	    case Cursor.DEFAULT_CURSOR:
		break;
	    case Cursor.MOVE_CURSOR:
		if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
		    delta = (pressX - e.getX()) * scaleX;
		    model.setValue((int) (modelValue - delta));
		} else {
		    delta = -(pressY - e.getY()) * scaleY;
		    model.setValue((int) (modelValue - delta));
		}
		repaint();
		break;

	    case Cursor.E_RESIZE_CURSOR:
		delta = (pressX - e.getX()) * scaleX;
		int extent = (int) (modelExtent - delta);
		if (extent < 0) {
		    setValue(modelValue + extent);
		    model.setExtent(0);
		} else {
		    model.setExtent(extent);
		}
		repaint();
		break;

	    case Cursor.W_RESIZE_CURSOR:
		delta = (pressX - e.getX()) * scaleX;
		if (delta > modelValue) {
		    delta = modelValue;
		}
		setValue((int) (modelValue - delta));
		repaint();
		break;

	    case Cursor.N_RESIZE_CURSOR:
		delta = -(pressY - e.getY()) * scaleY;
		extent = (int) (modelExtent - delta);
		if (extent < 0) {
		    setValue(modelValue + extent);
		    model.setExtent(0);
		} else {
		    model.setExtent(extent);
		}
		repaint();
		break;

	    case Cursor.S_RESIZE_CURSOR:
		delta = -(pressY - e.getY()) * scaleY;
		if (delta > modelValue) {
		    delta = modelValue;
		}
		setValue((int) (modelValue - delta));
		repaint();
		break;
	    }
	}

	@Override
	public void mousePressed(MouseEvent e) {
	    pressX = e.getX();
	    pressY = e.getY();
	    modelValue = model.getValue();
	    modelExtent = model.getExtent();
	}
    }

    private static final long serialVersionUID = -4923076507643832793L;

    private BoundedRangeModel model = new DefaultBoundedRangeModel();

    private MouseHandler mouseHandler = new MouseHandler();
    private float scaleX, scaleY;
    private Rectangle thumbRect, middleRect, extentThumbRect;

    private JSlider slider = new JSlider();

    public JRangeSlider() {
	this(0, 100, 0, 10);
    }

    public JRangeSlider(int min, int max, int value, int extent) {
	model.setMinimum(min);
	model.setMaximum(max);
	model.setValue(value);
	model.setExtent(extent);

	slider.setUI(new RangeSliderUI(slider));
	slider.setMinimum(min);
	slider.setMaximum(max);

	addMouseListener(mouseHandler);
	addMouseMotionListener(mouseHandler);

	addComponentListener(new ComponentAdapter() {
	    @Override
	    public void componentResized(ComponentEvent e) {
		computeScaleX();
		computeScaleY();
	    }
	});
	setBorder(new EmptyBorder(1, 1, 1, 1));
	model.addChangeListener(new ChangeListener() {
	    public void stateChanged(ChangeEvent e) {
		fireChangeEvent();
		repaint();
	    }
	});
    }

    public int getValue() {
	return model.getValue();
    }

    public void setValue(int i) {
	i = clamp(i);
	int v = model.getValue();
	int e = model.getExtent();
	model.setRangeProperties(i, v + e - i, model.getMinimum(), model.getMaximum(), false);
    }

    private int clamp(int i) {
	int max = model.getMaximum();
	if (i > max) {
	    i = max;
	}
	int min = model.getMinimum();
	if (i < min) {
	    i = min;
	}
	return i;
    }

    public int getSecondValue() {
	return model.getValue() + model.getExtent();
    }

    public void setSecondValue(int i) {
	i = clamp(i);

	int v = model.getValue();
	model.setExtent(i - v);
    }

    private void fireChangeEvent() {
	EventListenerListIterator<ChangeListener> iter = new EventListenerListIterator<ChangeListener>(ChangeListener.class, listenerList);
	ChangeEvent e = new ChangeEvent(this);
	while (iter.hasNext()) {
	    ChangeListener next = iter.next();
	    next.stateChanged(e);
	}
    }

    @Override
    protected void paintComponent(Graphics g) {
	super.paintComponent(g);

	RangeSliderUI ui = (RangeSliderUI) slider.getUI();
	slider.setBounds(getBounds());

	slider.setValue(model.getValue() + model.getExtent());
	slider.paint(g);
	extentThumbRect = ui.getThumbRect();

	slider.setValue(model.getValue());
	ui.paintThumb(g);
	thumbRect = ui.getThumbRect();

	Rectangle fillRect;
	switch (slider.getOrientation()) {
	    case SwingConstants.HORIZONTAL:
		middleRect = new Rectangle(thumbRect);
		middleRect.x += thumbRect.width;
		middleRect.width = extentThumbRect.x - thumbRect.x - thumbRect.width;

		fillRect = new Rectangle(middleRect);
		fillRect.height = middleRect.height/4;
		fillRect.y += (middleRect.height-fillRect.height)/2;
		break;
	    case SwingConstants.VERTICAL:
	    default:
		middleRect = new Rectangle(extentThumbRect);
		middleRect.y += extentThumbRect.height;
		middleRect.height = thumbRect.y - extentThumbRect.y - extentThumbRect.height;

		fillRect = new Rectangle(middleRect);
		fillRect.width = middleRect.width/4;
		fillRect.x += (middleRect.width-fillRect.width)/2;
		break;
	}

	paintTrackRange(g, fillRect);
    }

    protected void paintTrackRange(Graphics g, Rectangle rect) {
	g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    private void computeScaleX() {
	float width = getWidth();
	Insets ins = getInsets();
	width -= ins.left + ins.right;

	int min = model.getMinimum();
	int max = model.getMaximum();

	float size = max - min;
	scaleX = size / width;
    }

    private void computeScaleY() {
	float height = getHeight();
	Insets ins = getInsets();
	height -= ins.top + ins.bottom;

	int min = model.getMinimum();
	int max = model.getMaximum();

	float size = max - min;
	scaleY = size / height;
    }

    // all following methods just forwarding calls to/from JSlider

    @SuppressWarnings("rawtypes")
    public Dictionary getLabelTable() {
	return slider.getLabelTable();
    }

    @SuppressWarnings("rawtypes")
    public void setLabelTable(Dictionary labels) {
	slider.setLabelTable(labels);
    }

    public boolean getPaintLabels() {
	return slider.getPaintLabels();
    }

    public void setPaintLabels(boolean b) {
	slider.setPaintLabels(b);
    }

    public boolean getPaintTrack() {
	return slider.getPaintTrack();
    }

    public void setPaintTrack(boolean b) {
	slider.setPaintTrack(b);
    }

    public boolean getPaintTicks() {
	return slider.getPaintTicks();
    }

    public void setPaintTicks(boolean b) {
	slider.setPaintTicks(b);
    }

    public boolean getSnapToTicks() {
	return slider.getSnapToTicks();
    }

    public void setSnapToTicks(boolean b) {
	slider.setSnapToTicks(b);
    }

    public int getMinorTickSpacing() {
	return slider.getMinorTickSpacing();
    }

    public void setMinorTickSpacing(int n) {
	slider.setMinorTickSpacing(n);
    }

    public int getMajorTickSpacing() {
	return slider.getMajorTickSpacing();
    }

    public void setMajorTickSpacing(int n) {
	slider.setMajorTickSpacing(n);
    }

    public boolean getInverted() {
	return slider.getInverted();
    }

    public void setInverted(boolean b) {
	slider.setInverted(b);
    }

    public void setFont(Font font) {
	if (slider != null) {
	    slider.setFont(font);
	}
    }

    @SuppressWarnings("rawtypes")
    public Hashtable createStandardLabels(int increment, int start) {
	return slider.createStandardLabels(increment, start);
    }

    @SuppressWarnings("rawtypes")
    public Hashtable createStandardLabels(int increment) {
	return slider.createStandardLabels(increment);
    }

    @Override
    public Dimension getPreferredSize() {
	return slider.getPreferredSize();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
	slider.setPreferredSize(preferredSize);
    }

    public int getOrientation() {
	return slider.getOrientation();
    }

    public void setOrientation(int orientation) {
	slider.setOrientation(orientation);
    }

    public void addChangeListener(ChangeListener l) {
	listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
	listenerList.remove(ChangeListener.class, l);
    }

    public boolean getValueIsAdjusting() {
	return slider.getValueIsAdjusting();
    }

    public void setValueIsAdjusting(boolean b) {
	slider.setValueIsAdjusting(b);
    }

    public int getMaximum() {
	return slider.getMaximum();
    }

    public void setMaximum(int maximum) {
	model.setMaximum(maximum);
	slider.setMaximum(maximum);
    }

    public int getMinimum() {
	return slider.getMinimum();
    }

    public void setMinimum(int minimum) {
	model.setMinimum(minimum);
	slider.setMinimum(minimum);
    }

    public BoundedRangeModel getModel() {
	return model;
    }

    public void setModel(BoundedRangeModel newModel) {
	this.model = newModel;
	slider.setMinimum(model.getMinimum());
	slider.setMaximum(model.getMaximum());
    }

    public ChangeListener[] getChangeListeners() {
	return listenerList.getListeners(ChangeListener.class);
    }

    public static void main(String... s) {
	JFrame frame = new JFrame();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().setLayout(new FlowLayout());
	final JRangeSlider jrs = new JRangeSlider(0, 100, 20, 30);
	jrs.setOrientation(SwingConstants.VERTICAL);

	final JToggleButton jtb = new JToggleButton("ChangeValue");
	jtb.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		if (jtb.isSelected()) {
		    jrs.setValue(30);
		} else {
		    jrs.setValue(70);
		}
	    }
	});
	frame.getContentPane().add(jrs);
	frame.getContentPane().add(jtb);
	frame.getContentPane().add(new JSlider());

	frame.pack();
	frame.setVisible(true);
    }
}
