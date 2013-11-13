package vlcj.fx;

import java.nio.ByteBuffer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritablePixelFormat;
import javafx.util.Duration;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;
import vidada.model.settings.DatabaseSettings;
import vidada.viewsFX.player.IMediaController;
import vlcj.VLCMediaController;
import archimedesJ.util.Lists;

import com.sun.jna.Memory;

/**
 * Represents the VLC MediaPlayer
 * @author IsNull
 *
 */
public class MediaPlayerVLC extends Canvas 
{
	// TODO refactore this dependency away
	private final DatabaseSettings applicationSettings = DatabaseSettings.getSettings();

	// player factory config
	private static String[] args = {"--no-plugins-cache",  "--no-video-title-show", "--no-snapshot-preview", "--quiet", "--quiet-synchro", "--intf", "dummy"};
	private static String[] directPlaySund = {"--no-audio"};

	/**
	 * Media controller for the current player
	 */
	private final VLCMediaController mediaController = new VLCMediaController();

	/**
	 * Pixel writer to update the canvas.
	 */
	private final PixelWriter pixelWriter;

	/**
	 * Pixel format.
	 */
	private final WritablePixelFormat<ByteBuffer> pixelFormat;

	private MediaPlayerFactory vlcfactory;
	private MediaPlayer vlcMediaPlayer;

	// FX rendering
	private Timeline t;
	private volatile ByteBuffer tmpbuffer = null;
	private volatile BufferFormat tmpformat = null;


	/**
	 * Creates a new MediaPlayerVLC canvas
	 */
	public MediaPlayerVLC(){
		pixelWriter = this.getGraphicsContext2D().getPixelWriter();
		pixelFormat = PixelFormat.getByteBgraInstance();

		getMediaPlayer(); // ensure the media player is loaded
	}


	public IMediaController getMediaController(){
		return mediaController;
	}

	/**
	 * Gets the MediaPlayer, it will be created if no cached instance is avaiable
	 * @return
	 */
	public MediaPlayer getMediaPlayer(){
		if(vlcMediaPlayer == null){
			vlcMediaPlayer = createMediaPlayer();
			mediaController.bind(vlcMediaPlayer);
			vlcMediaPlayer.addMediaPlayerEventListener(mediaPlayerEventListener);
		}
		return vlcMediaPlayer;
	}


	private final MediaPlayerEventListener mediaPlayerEventListener = new MediaPlayerEventAdapter(){

		@Override
		public void playing(MediaPlayer mediaPlayer) {
			// when vlc starts rendering, we have to start our FX rendering
			renderMovie(mediaPlayer.getFps());
		}

		@Override
		public void stopped(MediaPlayer mediaPlayer) {
			// we can stop updating the back buffer
			stopRender();
		}
	};

	/**
	 * Render the back-buffer into the canvas with the given fps
	 * @param fps Update interval for the rendering
	 */
	private void renderMovie(final double fps){

		System.out.println("MediaPlayerVLC: render movie @ " + fps + " fps");

		if(t != null)
			stopRender();

		t = new Timeline();
		t.setCycleCount(Timeline.INDEFINITE);

		double duration = 1000 / fps;

		t.getKeyFrames().add(
				new KeyFrame(Duration.millis(duration),
						onFinished));

		t.playFromStart();
	}

	private void stopRender(){
		System.out.println("MediaPlayerVLC: stoped render movie");
		t.stop();
		t = null;
	}


	private final EventHandler<ActionEvent> onFinished = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent t) {
			updateFrame();
		}
	};

	/**
	 * Updates the current frame (copy back-buffer to the canvas)
	 * 
	 * Note: This method must be called in the FX Application thread
	 */
	private final void updateFrame(){

		// save the references (to avoid NPE race conditions)
		final ByteBuffer currentBuffer = tmpbuffer;
		final BufferFormat currentFormat = tmpformat;

		if(currentBuffer != null && currentFormat != null)
		{
			pixelWriter.setPixels(0, 0, 
					currentFormat.getWidth(),
					currentFormat.getHeight(),
					pixelFormat, currentBuffer,
					currentFormat.getWidth()*4);
		}
	}


	private MediaPlayer createMediaPlayer(){
		MediaPlayer mediaPlayer = null;
		MediaPlayerFactory playerFactory = getMediaPlayerFactory();
		mediaPlayer = playerFactory.newDirectMediaPlayer(formatCallback, renderCallback);
		return mediaPlayer;
	}

	private MediaPlayerFactory getMediaPlayerFactory(){
		if(vlcfactory == null){
			vlcfactory = createFactory();
			releasePlayer();
		}
		return vlcfactory;
	}



	private MediaPlayerFactory createFactory(){
		System.out.println("creating new MediaPlayerFactory...");

		String[] myargs = args;

		if(!applicationSettings.isPlaySoundDirectPlay())
			myargs = Lists.concat(args, directPlaySund);

		return new MediaPlayerFactory(myargs);
	}

	private void releasePlayer(){
		if(vlcMediaPlayer != null){ 
			vlcMediaPlayer.release();
			vlcMediaPlayer.removeMediaPlayerEventListener(mediaPlayerEventListener);
			mediaController.unbind(vlcMediaPlayer);
			vlcMediaPlayer = null;
		}
	}

	private void releaseFactory(){
		if(vlcfactory != null){
			vlcfactory.release();
			vlcfactory = null;
		}
	}

	/**
	 * Callback to get the buffer format to use for video playback.
	 */
	private final BufferFormatCallback formatCallback = new BufferFormatCallback() {
		@Override
		public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {

			int width = (int)getWidth();
			int height = (int)getHeight();
			System.out.println("MediaPlayerVLC:getBufferFormat: Media(" + width + " x " + height + ")");

			// TODO: double size for retina displays
			return new RV32BufferFormat(width, height);
		}
	};


	/**
	 * Callback to get the buffer format to use for video playback.
	 */
	private final RenderCallback renderCallback = new RenderCallback() {

		@Override
		public void display(DirectMediaPlayer mediaPlayer,
				Memory[] nativeBuffers, final BufferFormat bufferFormat) {
			final Memory nativeBuffer = nativeBuffers[0];
			final ByteBuffer byteBuffer = nativeBuffer.getByteBuffer(0, nativeBuffer.size());

			// we just store the latest references
			tmpbuffer = byteBuffer;
			tmpformat = bufferFormat;
		}
	};

}