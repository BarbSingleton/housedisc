package com.github.pkaeding.housedisc;

import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBufferByte;
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
		Iterator<ImageReader> ir = ImageIO.getImageReadersByFormatName("pbm");
		while (ir.hasNext()) {
			ImageReader r = ir.next();
			System.out.println("can read raster: " + r.canReadRaster());
			System.out.println(r);
		}
		String filename = args[0];
		try {
			BufferedImage inputImage = ImageIO.read(ImageIO
					.createImageInputStream(new File(filename)));
			processPage(inputImage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Hello World!");
	}

	static void processPage(BufferedImage page) throws IOException {
		String id = UUID.randomUUID().toString();
		BufferedImage imgGray = toGrayscale(page);
		ImageIO.write(imgGray, "png", new File("/tmp/" + id + "-gray.png"));
		BufferedImage img = rotateLeft90degrees(imgGray);
		ImageIO.write(img, "png", new File("/tmp/" + id + "-rotated.png"));
		System.out.println(img.getColorModel().getPixelSize() + " bits");
		System.out.println(img.getColorModel().getColorSpace()
				.getNumComponents()
				+ " components");
		int rows = img.getWidth();
		int cols = img.getHeight();
		Mat mat = new Mat(rows, cols, CvType.CV_8UC1);// CV_8UC3);
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer())
				.getData();
		mat.put(0, 0, pixels);
		Highgui.imwrite("/tmp/" + id + "-input.png", mat);
		Mat edges = new Mat();
		Imgproc.Canny(mat, edges, 50, 200);
		Highgui.imwrite("/tmp/" + id + "-canny.png", edges);
		Mat lines = new Mat();
		Imgproc.HoughLines(edges, lines, 1.0, Math.PI / 180, 10);
		System.out.println(lines);
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
						BufferedImage.TYPE_BYTE_GRAY));
	}
	
	static Comparator<Mat> houghLineComparator = new Comparator<Mat>() {
		public int compare(Mat o1, Mat o2) {
			// TODO Auto-generated method stub
			// return o1.get(0, 0, null) - o2.get(0, 0, null);
			return 0;
		}
	};
}
