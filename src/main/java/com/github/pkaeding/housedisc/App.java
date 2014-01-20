package com.github.pkaeding.housedisc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * Hello world!
 * 
 */
public class App {
	static {
		System.loadLibrary("opencv_java248");
	}

	public static void main(String[] args) {
		ImageIO.scanForPlugins();
		IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
		String filename = args[0];
		try {
			BufferedImage inputImage = ImageIO.read(ImageIO
					.createImageInputStream(new File(filename)));
			processPage2(inputImage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Scans inward from the left of the image, looking for a lot of black pixels.  Just how many 
	 * is a lot is determined by the threshold provided
	 * @param image
	 * @param threshold between 0 and 1.0, the ratio that will be the minimum to be considered a
	 * border.  For instance, if you want to find a column that is 80% black pixels, pass in 0.8.
	 * @return the x coordinate that has more than the threshold black pixels, or -1 if no such 
	 * colum was found.
	 */
	static int findLeftBorder(BufferedImage image, double threshold) {
		Raster raster = image.getData();
		int minX = raster.getMinX();
		int minY = raster.getMinY();
		int maxX = raster.getWidth() + minX;
		int height = raster.getHeight();
		boolean foundBlack = false;
		for (int i = minX; i < maxX; i++) {
			int[] pixels = raster.getPixels(i, minY, 1, height, new int[height]);
			int numBlack = countBlackPixels(pixels);
			if (((double) numBlack) / height > threshold) {
				foundBlack = true;
			} else if (foundBlack == true) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Scans inward from the right of the image, looking for a lot of black pixels.  Just how many 
	 * is a lot is determined by the threshold provided
	 * @param image
	 * @param threshold between 0 and 1.0, the ratio that will be the minimum to be considered a
	 * border.  For instance, if you want to find a column that is 80% black pixels, pass in 0.8.
	 * @return the x coordinate that has more than the threshold black pixels, or -1 if no such 
	 * colum was found.
	 */
	static int findRightBorder(BufferedImage image, double threshold) {
		Raster raster = image.getData();
		int minX = raster.getMinX();
		int minY = raster.getMinY();
		int maxX = raster.getWidth() + minX;
		int height = raster.getHeight() - 1;
		boolean foundBlack = false;
		for (int i = maxX - 1; i >= minX; i--) {
			int[] pixels = raster.getPixels(i, minY, 1, height, new int[height]);
			int numBlack = countBlackPixels(pixels);
			if (((double) numBlack) / height > threshold) {
				foundBlack = true;
			} else if (foundBlack == true) {
				return i;
			}
		}
		return -1;
	}
	
	static int countBlackPixels(int[] pixels) {
		int ret = 0;
		for (int i = 0; i < pixels.length; i++) {
			if ((pixels[i] & 0x00FFFFFF) == 0) {
				ret++;
			}
		}
		return ret;
	}
	
	static void processPage2(BufferedImage page) throws IOException {
		String id = UUID.randomUUID().toString();
		BufferedImage rotated = rotateLeft90degrees(page);
		ImageIO.write(rotated, "png", new File("/tmp/" + id + "-rotated.png"));
		BufferedImage img = toGrayscale(rotated);
		ImageIO.write(img, "png", new File("/tmp/" + id + "-gray.png"));
		int leftBorder = findLeftBorder(img, 0.6);
		int rightBorder = findRightBorder(img, 0.6);
		Graphics2D graphics = rotated.createGraphics();
		graphics.setColor(Color.blue);
		int height = rotated.getHeight() - 1;
		
		if (leftBorder > 0)
			graphics.draw(new Line2D.Double(leftBorder, 0, leftBorder, height));
		else 
			System.out.println("no left border found");
		
		if (rightBorder > 0)
			graphics.draw(new Line2D.Double(rightBorder, 0, rightBorder, height));
		else 
			System.out.println("no right border found");
		
		ImageIO.write(rotated, "png", new File("/tmp/" + id + "-lines.png"));
	}

	static void processPage(BufferedImage page) throws IOException {
		String id = UUID.randomUUID().toString();
		BufferedImage rotated = rotateLeft90degrees(page);
		ImageIO.write(rotated, "png", new File("/tmp/" + id + "-rotated.png"));
		BufferedImage img = toGrayscale(rotated);
		ImageIO.write(img, "png", new File("/tmp/" + id + "-gray.png"));
		int cols = img.getWidth();
		int rows = img.getHeight();
		Mat mat = new Mat(rows, cols, CvType.CV_8UC1);
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer())
				.getData();
		mat.put(0, 0, pixels);
		Highgui.imwrite("/tmp/" + id + "-input.png", mat);
		Mat edges = new Mat();
		Imgproc.Canny(mat, edges, 50, 200);
		Highgui.imwrite("/tmp/" + id + "-canny.png", edges);
		Mat lines = new Mat();
		Imgproc.HoughLinesP(mat, lines, 1.0, Math.PI / 180, 50, 50, 10);
		System.out.println(lines);
		
		Graphics2D graphics = rotated.createGraphics();
		graphics.setColor(Color.blue);
		System.out.println("lines: " + lines.dump());
		System.out.println("found " + lines.rows() + " lines");
		System.out.println("found " + lines.cols() + " cols");
		for (int i = 0; i < lines.cols() && i < 5; i++) 
		{
			double[] vec = lines.get(0, i);
			double y1 = vec[0];
			double x1 = vec[1];
			double y2 = vec[2];
			double x2 = vec[3];
	          
//			if (Math.abs(y1 - y2) < 20) {
				System.out.println(String.format("p1: (%s, %s) p2: (%s, %s)",
						x1, y1, x2, y2));
				graphics.draw(new Line2D.Double(x1, y1, x2, y2));
//			}
		}
		ImageIO.write(rotated, "png", new File("/tmp/" + id + "-lines.png"));
		// Raster data = img.getData();
		// data.getp
		// data.getp
	}

	static BufferedImage toGrayscale(BufferedImage source) {
		BufferedImageOp op = new ColorConvertOp(
				ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		return op.filter(source,
				new BufferedImage(source.getWidth(), source.getHeight(),
						BufferedImage.TYPE_BYTE_GRAY));
	}

	static BufferedImage rotateLeft90degrees(BufferedImage source) {
		AffineTransform tx = new AffineTransform();
		tx.translate(source.getHeight() / 2, source.getWidth() / 2);
		tx.rotate(Math.toRadians(-90));
		tx.translate(-source.getWidth() / 2, -source.getHeight() / 2);
		BufferedImageOp op = new AffineTransformOp(tx,
				AffineTransformOp.TYPE_BICUBIC);
		return op.filter(source,
				new BufferedImage(source.getHeight(), source.getWidth(),
						BufferedImage.TYPE_BYTE_INDEXED));
	}

	static Comparator<Mat> houghLineComparator = new Comparator<Mat>() {
		public int compare(Mat o1, Mat o2) {
			// TODO Auto-generated method stub
			// return o1.get(0, 0, null) - o2.get(0, 0, null);
			return 0;
		}
	};
}
