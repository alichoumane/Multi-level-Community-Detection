package iotools;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import utils.Vector;

public class ColorFactory {

	public static CustomLogger logger = new CustomLogger("ColorFactory", Level.FINER);
	
	public static ArrayList<String> getColors(int n){
		ArrayList<String> colors = new ArrayList<>();
		if(n==2) {
			colors.add(String.format("#%02x%02x%02x",75,230,0));
			colors.add(String.format("#%02x%02x%02x",255,42,20));
			return colors;
		}/*else if(n==5) {
			colors.add(String.format("#%02x%02x%02x",256,110,120));
			colors.add(String.format("#%02x%02x%02x",35,190,0));
			colors.add(String.format("#%02x%02x%02x",40,195,0));
			colors.add(String.format("#%02x%02x%02x",45,200,0));
			colors.add(String.format("#%02x%02x%02x",256,115,125));
			return colors;
		}*/
		double partitions = Math.pow(n+2,1/3.0);
		int parts = (int) Math.floor(partitions);
		if(partitions-parts!=0) {
			parts++;
		}
		int R,G,B;
		for(int i=1;i<=parts;i++){
			for(int j=1;j<=parts;j++){
				for(int k=1;k<=parts;k++){
					R=(int) ((255.0/parts)*i);
					G=(int) ((255.0/parts)*j);
					B=(int) ((255.0/parts)*k);
					if(R!=0.0 && G!=0.0 && B!=0.0 && (R!=B || R!=G || B!=G)){
						colors.add(String.format("#%02x%02x%02x",R,G,B));
					}
				}
			}
		}
		return colors;
	}

	/**
	 * 
	 * @param color1
	 * @param ratioOf1 determines how much the returned colour will be close to colour 1
	 * @param color2
	 * @param ratioOf2 determines how much the returned colour will be close to colour 2
	 * @return
	 */
	public static String getMiddleColor(String color1, double ratioOf1, String color2, double ratioOf2) {
		Color c1 = Color.decode(color1);
		Color c2 = Color.decode(color2);
		if(ratioOf1+ratioOf2>1.1) {
			logger.log(Level.WARNING, "color memberships sum to > 1, "+ratioOf1+"+"+ratioOf2+"="+(ratioOf1+ratioOf2));
			return String.format("#%02x%02x%02x",255,255,255);
		}
		int r = (int)(c1.getRed()*ratioOf1 + c2.getRed()*ratioOf2);
		int g = (int)(c1.getGreen()*ratioOf1 + c2.getGreen()*ratioOf2);
		int b = (int)(c1.getBlue()*ratioOf1 + c2.getBlue()*ratioOf2);
		return String.format("#%02x%02x%02x",r,g,b);
	}
	
	public static String getMiddleColor(ArrayList<String> colors, Vector<Integer, Double> ratios) {
		int r=0;
		int g=0;
		int b=0;
		Iterator<Integer> iterator = ratios.keySet().iterator();
		int i=0;
		do {
			int key = iterator.next();
			Color c1 = Color.decode(colors.get(i));
			r+=(int)(c1.getRed()*ratios.get(key));
			g+=(int)(c1.getGreen()*ratios.get(key));
			b+=(int)(c1.getBlue()*ratios.get(key));
			i++;
		}while(iterator.hasNext());
		return String.format("#%02x%02x%02x",r,g,b);
	}
	public static String getMiddleColor(ArrayList<String> colors, ArrayList<Double> ratios) {
		int r=0;
		int g=0;
		int b=0;
		for(int i=0;i<colors.size();i++) {
			Color c1 = Color.decode(colors.get(i));
			r+=(int)(c1.getRed()*ratios.get(i));
			g+=(int)(c1.getGreen()*ratios.get(i));
			b+=(int)(c1.getBlue()*ratios.get(i));
		}
		return String.format("#%02x%02x%02x",r,g,b);
	}
	
	/**
	 * used to decide the colour of a node based on its group
	 * @author Ali Harkous
	 *
	 */
	public class NodePainter{
		
		private boolean useGradients = true;
		private HashMap<String, Vector<Integer, Double>> memberships = null;
		public NodePainter(boolean useGradients, HashMap<String, Vector<Integer, Double>> memberships) {
			this.useGradients=useGradients;
			setMemberships(memberships);
		}
		public void setMemberships(HashMap<String, Vector<Integer, Double>> memberships) {
			this.memberships = memberships;
		}
		public String getColorOf(String node, HashMap<Integer, String> groupsColors) {
			Vector<Integer, Double> nodeMemberships = memberships.get(node);
			if(!useGradients) {
				//find max membership
				double max=0;
				Integer g=nodeMemberships.keySet().iterator().next();
				for(Integer key:nodeMemberships.keySet()){
					Double m = nodeMemberships.get(key);
					if(m>max) {
						max=m;
						g=key;
					}
				}
				return groupsColors.get(g);
			}else {
				return getMiddleColor(new ArrayList<String>(groupsColors.values()), nodeMemberships);
			}
		}
		
		public HashMap<Integer, String> getGroupsColors(ArrayList<Integer> groups){
			ArrayList<String> colors = getColors(groups.size());
			HashMap<Integer, String> groupsColors = new HashMap<>();
			int j=0;
			for(int i=0;i<groups.size();i++) {
				groupsColors.put(groups.get(i), (i%2==0)?colors.get(j++):colors.get(colors.size()-j));
			}
			return groupsColors;
		}
	}

	/**
	 * to test using a JFrame
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean testSpectrum=true;
		
		if(testSpectrum) {
			//test spectrum
			int nbColors = 6;
			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(700, 600);
			ArrayList<String> colors = getColors(nbColors);
			//Collections.shuffle(colors);
			frame.setLayout(new GridLayout(1, colors.size()));
			//ArrayList<JPanel> colorPanels = new ArrayList<JPanel>();
			int j=0;
			for(int i=0;i<colors.size();i++) {
				JPanel p = new JPanel();
				p.setBackground(Color.decode(/*colors.get(i)*/(i%2==0)?colors.get(j++):colors.get(colors.size()-j)));
				frame.add(p);
			}
			frame.setVisible(true);
		}else {
			//test gradient
			ArrayList<String> colors = getColors(2);
			ArrayList<Double> ratios = new ArrayList<Double>();
			for(int i=0;i<colors.size();i++) {
				ratios.add(1.0/colors.size());
			}
			
			JFrame frame2 = new JFrame();
			frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame2.setSize(700, 600);
			frame2.setLayout(new GridLayout(colors.size()+2, 1));
			
			JSlider slider = new JSlider(0, 100, 50);
			frame2.add(slider);
			JPanel p = new JPanel();
			p.setBackground(Color.decode(colors.get(0)));
			frame2.add(p);
			JPanel cp = new JPanel();
			//cp.setBackground(Color.decode(factory.getMiddleColor(colors.get(0), 0.5, colors.get(1), 0.5)));
			cp.setBackground(Color.decode(getMiddleColor(colors,ratios)));
			frame2.add(cp);
			p = new JPanel();
			p.setBackground(Color.decode(colors.get(1)));
			frame2.add(p);
			for(int i=2;i<colors.size();i++) {
				p = new JPanel();
				p.setBackground(Color.decode(colors.get(i)));
				frame2.add(p);
			}
			
			slider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					double s = slider.getValue()/100.0;
					double v1=0.5,v2=0.5;
					if(s>0.5) {
						s-=0.5;
						v2+=s;
						v1-=s;
					}else {
						s=0.5-s;
						v2-=s;
						v1+=s;
					}
					cp.setBackground(Color.decode(getMiddleColor(colors.get(0), v1, colors.get(1), v2)));
				}
			});
			frame2.setVisible(true);
		}
		
	}
}
