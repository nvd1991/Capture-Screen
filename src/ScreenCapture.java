import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import java.util.concurrent.*;

public class ScreenCapture {
    private JPEGImageWriteParam jpegImageWriteParam;
    private ImageWriter writer;
    private Vector<String> imagesLinks;

    public ScreenCapture(){
        imageWriterSetup();
    }

    private void imageWriterSetup(){
        //Setup image compression mode
        this.jpegImageWriteParam = new JPEGImageWriteParam(null);
        jpegImageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegImageWriteParam.setCompressionQuality(0.7f);

        this.writer = ImageIO.getImageWritersByFormatName("jpeg").next();

        this.imagesLinks = new Vector<>();
    }


    private void captureScreen() throws AWTException, IOException {
        //Use robot to snapshot screen
        Rectangle screenSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage capture = (new Robot()).createScreenCapture(screenSize);

        //Compress and write image to file
        String fileName = "output/" + System.currentTimeMillis() + ".jpg";
        FileImageOutputStream os = new FileImageOutputStream(new File(fileName));
        writer.setOutput(os);
        writer.write(null, new IIOImage(capture, null, null), jpegImageWriteParam);
        imagesLinks.add(fileName);
        os.close();
    }

    private void convertJPEGsToVideo() throws IOException {
        //Use Jcodec to convert images to video
        AWTSequenceEncoder awtSequenceEncoder = AWTSequenceEncoder.createSequenceEncoder(new File("output/output.mp4"), 60);
        for(int i = 0; i < imagesLinks.size(); i++){
            BufferedImage image = ImageIO.read(new File(imagesLinks.get(i)));
            awtSequenceEncoder.encodeImage(image);
        }
        awtSequenceEncoder.finish();
    }

    public static void main(String[] args){
        //Set up capture screen task that performs periodically
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScreenCapture screenCapture = new ScreenCapture();
        Runnable captureScreenTask = new Runnable() {
            @Override
            public void run() {
                try {
                    screenCapture.captureScreen();
                } catch (AWTException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Future<?> future = scheduledExecutorService.scheduleAtFixedRate(captureScreenTask,1000, 16, TimeUnit.MILLISECONDS);

        //Setup cancel task
        Runnable cancelTask = new Runnable() {
            @Override
            public void run() {
                future.cancel(true);
                try {
                    screenCapture.convertJPEGsToVideo();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        };
        scheduledExecutorService.schedule(cancelTask, 10000, TimeUnit.MILLISECONDS);
    }
}
