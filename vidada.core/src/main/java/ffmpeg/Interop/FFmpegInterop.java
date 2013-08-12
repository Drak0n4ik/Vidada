package ffmpeg.Interop;

import java.awt.Dimension;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.joda.time.Duration;

import vidada.model.video.VideoInfo;
import archimedesJ.io.ShellExecute;
import archimedesJ.util.FileSupport;
import archimedesJ.util.OSValidator;
import ffmpeg.FFmpegException;

/**
 * Platform independent ffmpeg access
 * @author IsNull
 *
 */
public abstract class FFmpegInterop {

	protected static File encoder;

	private static FFmpegInterop instance = null;
	private static int DEFAULT_TIMEOUT = 1000 * 5; // ms


	public static FFmpegInterop instance(){

		if(instance == null){
			if(OSValidator.isWindows()){
				instance = new FFmpegInteropWindows();
			}else {
				instance = new FFmpegInteropLinux();
			}

			System.out.println("loaded ffmpeg interop: " + instance.getClass().getName());
		}



		return instance;
	}


	/**
	 *  Extracts the frame at the given time as an image.
	 *  
	 * @param pathToVideo
	 * @param pathToImage
	 * @param second
	 * @param size
	 * @throws FFmpegException
	 */
	public void createImage(URI pathToVideo, File pathToImage, int second, Dimension size) throws FFmpegException {

		//if(!pathToVideo.exists())
		//	throw new IllegalArgumentException("file must exist and being readable! @ " + pathToVideo.toString());

		FileUtils.deleteQuietly(pathToImage);

		List<String> argumentBuilder = new ArrayList<String>();

		argumentBuilder.add("-ss"); 		// seek - better before setting the -i input! 
		argumentBuilder.add(second +"");

		argumentBuilder.add("-i");			// input video
		argumentBuilder.add(shieldPathArgument(pathToVideo));

		argumentBuilder.add("-an");			// no audio

		argumentBuilder.add("-frames:v");	// frames to extract
		argumentBuilder.add("1");

		/*
		argumentBuilder.add("-f");			// format
		argumentBuilder.add("mjpeg");		//image2 / mjpeg
		 */
		argumentBuilder.add("-y");			// overwrite existing thumb


		argumentBuilder.add("-s"); 			// thumb size
		argumentBuilder.add(dimensionToString(size));

		argumentBuilder.add(shieldPathArgument(pathToImage));

		String log = ffmpegExec(argumentBuilder, DEFAULT_TIMEOUT);


		if(!pathToImage.exists())
		{
			throw new FFmpegException("Image could not been created.(file missing)", log);
		}

	}	


	//
	// define the patterns to extract the information
	//
	private static final Pattern regex_Duration = Pattern.compile("Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)");
	private static final Pattern regex_BitRate = Pattern.compile("bitrate: (\\d*) kb/s");
	private static final Pattern regex_Resolution = Pattern.compile("Video: .* (\\d{2,})x(\\d{2,})");

	public VideoInfo getVideoInfo(URI pathToVideo) {

		//if(!pathToVideo.exists())
		//	throw new IllegalArgumentException("file must exist and being readable! @ " + pathToVideo.toString());

		List<String> argumentBuilder = new ArrayList<String>();

		argumentBuilder.add("-ss"); 		// seek - 
		argumentBuilder.add("2");

		argumentBuilder.add("-i");
		argumentBuilder.add(shieldPathArgument(pathToVideo));

		argumentBuilder.add("-an");			// no audio

		argumentBuilder.add("-frames:v");	// frames to extract
		argumentBuilder.add("1");

		String log = ffmpegExec(argumentBuilder, DEFAULT_TIMEOUT);

		Duration videoDuration = null;
		int videoBitrate = 0;
		Dimension resolution = null;


		//
		// parse duration
		//
		Matcher m = regex_Duration.matcher(log);
		if(m.find()){
			// Duration: 02:41:41.68,

			Duration hours = Duration.standardHours(Integer.parseInt(m.group(1)));
			Duration minutes = Duration.standardMinutes(Integer.parseInt(m.group(2)));
			Duration seconds = Duration.standardSeconds(Integer.parseInt(m.group(3)));

			videoDuration = Duration.millis(hours.getMillis() + minutes.getMillis() + seconds.getMillis());
		}else {
			System.err.println("duration info not found!");
		}

		//
		// parse duration
		//
		m = regex_BitRate.matcher(log);
		if(m.find()){
			// bitrate: 10731 kb/s
			videoBitrate = Integer.parseInt(m.group(1));

		}else {
			System.err.println("bitrate info not found!");
		}


		//
		// parse native resolution
		//
		m = regex_Resolution.matcher(log);
		if(m.find()){
			resolution = new Dimension( 
					Integer.parseInt(m.group(1)),
					Integer.parseInt(m.group(2)));
		}else {
			System.err.println("resolution info not found!");
		}

		return new VideoInfo(videoDuration, videoBitrate, resolution);
	}


	/**
	 * Execute the given ffmpeg command
	 * @param args
	 * @param timeout
	 * @return
	 */
	private String ffmpegExec(List<String> args, long timeout){

		StringBuilder output = new StringBuilder();

		try {
			args.add(0, getFFmpegCMD());
			String[] command = args.toArray(new String[0]);
			String commandLine =  toCommandLine(args);
			System.out.println("shell: " + commandLine);
			output.append("shell: " + commandLine + FileSupport.NEWLINE);

			System.out.println("ffmpeg running. thread is waiting...");

			int exitVal = ShellExecute.executeAndWait(command, output, true, timeout);

			System.out.println("ffmpeg Exited with error code " + exitVal);

		} catch( TimeoutException e){
			e.printStackTrace();
		} catch( InterruptedException e){
			e.printStackTrace();
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}

		return output.toString();
	} 

	private String toCommandLine(Iterable<String> str){
		StringBuilder sb = new StringBuilder();
		for (String string : str) {
			sb.append(string + " ");
		}
		return sb.toString();
	}

	protected String dimensionToString(Dimension size){
		return  size.width + "x" + size.height;
	}

	protected String shieldPathArgument(File pathArg){
		return "\"" + pathArg.getAbsolutePath() + "\"";
	}

	protected String shieldPathArgument(URI pathArg){
		return "\"" + pathArg.getPath() + "\"";
	}

	/**
	 * Is ffmpeg avaiable
	 * @return
	 */
	public abstract boolean isAvaiable();

	public abstract File getFFmpegBinaryFile();

	protected abstract String getFFmpegCMD();




}
