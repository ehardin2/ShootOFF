package com.shootoff.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.geometry.Bounds;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;

import com.shootoff.camera.autocalibration.AutoCalibrationManager;
import com.shootoff.camera.shotdetection.JavaShotDetector;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.CalibrationManager;
import com.shootoff.gui.MockCanvasManager;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.gui.controller.ProjectorArenaController;

public class TestAutoCalibration {
	private AutoCalibrationManager acm;

	private Configuration config;
	private MockCanvasManager mockCanvasManager;
	private boolean[][] sectorStatuses;

	@Rule public ErrorCollector collector = new ErrorCollector();

	@Before
	public void setUp() throws ConfigurationException {
		nu.pattern.OpenCV.loadShared();

		acm = new AutoCalibrationManager(new MockCameraManager(), false);

		config = new Configuration(new String[0]);
		config.setDebugMode(false);
		mockCanvasManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[JavaShotDetector.SECTOR_ROWS][JavaShotDetector.SECTOR_COLUMNS];

		for (int x = 0; x < JavaShotDetector.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < JavaShotDetector.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
	}

	private Boolean autoCalibrationVideo(String videoPath) {
		Object processingLock = new Object();
		File videoFile = new File(TestCameraManagerLifecam.class.getResource(videoPath).getFile());

		MockCameraManager cameraManager;
		cameraManager = new MockCameraManager(videoFile, processingLock, mockCanvasManager, config, sectorStatuses,
				Optional.empty());

		mockCanvasManager.setCameraManager(cameraManager);

		cameraManager.setCalibrationManager(
				new CalibrationManager(new ShootOFFController(), cameraManager, new ProjectorArenaController()));
		cameraManager.enableAutoCalibration(false);
		cameraManager.processVideo();

		try {
			synchronized (processingLock) {
				while (!cameraManager.isVideoProcessed())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return cameraManager.cameraAutoCalibrated;
	}

	@Test
	public void testCalibrateProjection() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection.png"));

		
		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(32, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(422, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(318, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);

		assertEquals(false, acm.getPerspMat() == null);

		double[][] expectedMatrix = { { 1.03, 0.02, -11.20 }, { -0.00, 1.04, -7.04 }, { 0.00, 0.00, 1.00 } };

		for (int i = 0; i < acm.getPerspMat().rows(); i++) {
			for (int j = 0; j < acm.getPerspMat().cols(); j++) {
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i, j)[0], .1);
			}
		}

		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
	}

	@Test
	public void testCalibrateProjection2() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2.png"));

		
		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));


		assertTrue(calibrationBounds.isPresent());

		assertEquals(113, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(34, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(420, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(316, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);

		assertEquals(false, acm.getPerspMat() == null);

		double[][] expectedMatrix = { { 1.04, 0.03, -15.58 }, { -0.00, 1.04, -6.44 }, { 0.00, 0.00, 1.00 } };

		for (int i = 0; i < acm.getPerspMat().rows(); i++) {
			for (int j = 0; j < acm.getPerspMat().cols(); j++) {
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i, j)[0], .1);
			}
		}

		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-2-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));

	}

	@Test
	public void testCalibrateProjectionCutoff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/calibrate-projection-cutoff.png"));

		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertEquals(false, calibrationBounds.isPresent());

	}

	@Test
	public void testCalibrateTightPatternUpsidedown() throws IOException {
		BufferedImage testFrame = ImageIO.read(TestAutoCalibration.class
				.getResourceAsStream("/autocalibration/tight-calibration-pattern-upsidedown.png"));

		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertEquals(false, calibrationBounds.isPresent());

	}

	@Test
	public void testCalibrateTightPatternCutOff() throws IOException {
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-cutoff.png"));

		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertEquals(false, calibrationBounds.isPresent());

	}

	@Test
	public void testCalibrateTightPattern() throws IOException {
		BufferedImage testFrame = ImageIO
				.read(TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern.png"));

		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertTrue(calibrationBounds.isPresent());

		assertEquals(45, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(25, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(572, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(431, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);

		assertEquals(false, acm.getPerspMat() == null);

		double[][] expectedMatrix = { { 1.00, 0.00, -1.66 }, { 0.00, 1.00, -1.39 }, { 0.00, 0.00, 1.00 } };

		for (int i = 0; i < acm.getPerspMat().rows(); i++) {
			for (int j = 0; j < acm.getPerspMat().cols(); j++) {
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i, j)[0], .1);
			}
		}

		BufferedImage compareFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));

	}

	@Test
	public void testCalibrateTightPatternTurned() throws IOException {
		BufferedImage testFrame = ImageIO.read(
				TestAutoCalibration.class.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned.png"));

		Mat mat = Camera.bufferedImageToMat(testFrame);
		
		// Step 1: Find the chessboard corners
		final Optional<MatOfPoint2f> boardCorners = acm.findChessboard(mat);

		assertTrue(boardCorners.isPresent());

		
		Optional<Bounds> calibrationBounds = acm.calibrateFrame(boardCorners.get(), Camera.bufferedImageToMat(testFrame));

		assertTrue(calibrationBounds.isPresent());

		assertEquals(116, calibrationBounds.get().getMinX(), 1.0);
		assertEquals(88, calibrationBounds.get().getMinY(), 1.0);
		assertEquals(422, calibrationBounds.get().getWidth(), 1.0);
		assertEquals(298, calibrationBounds.get().getHeight(), 1.0);

		BufferedImage resultFrame = acm.undistortFrame(testFrame);

		assertEquals(false, acm.getPerspMat() == null);

		double[][] expectedMatrix = { { 0.88, -0.34, 89.04 }, { 0.24, 0.80, -55.97 }, { -0.00, -0.00, 1.00 } };

		for (int i = 0; i < acm.getPerspMat().rows(); i++) {
			for (int j = 0; j < acm.getPerspMat().cols(); j++) {
				assertEquals(expectedMatrix[i][j], acm.getPerspMat().get(i, j)[0], .1);
			}
		}

		BufferedImage compareFrame = ImageIO.read(TestAutoCalibration.class
				.getResourceAsStream("/autocalibration/tight-calibration-pattern-turned-result.png"));

		assertEquals(true, compareImages(compareFrame, resultFrame));
	}

	@Test
	public void testCalibrateHighRes() throws IOException {
		Boolean result = autoCalibrationVideo("/autocalibration/highres-autocalibration-1280x720.mp4");
		assertEquals(true, result);
	}
	
	/*
	 * http://stackoverflow.com/questions/11006394/is-there-a-simple-way-to-
	 * compare -bufferedimage-instances
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		// The images must be the same size.
		if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
			int width = imgA.getWidth();
			int height = imgA.getHeight();

			// Loop over every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Compare the pixels for equality.
					if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}
}
