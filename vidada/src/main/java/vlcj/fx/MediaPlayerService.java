package vlcj.fx;

import vidada.viewsFX.player.IMediaPlayerService;
import vidada.viewsFX.player.MediaPlayerFx;
import vlcj.VlcjUtil;
import archimedesJ.events.EventArgs;
import archimedesJ.events.EventHandlerEx;
import archimedesJ.events.IEvent;

public class MediaPlayerService implements IMediaPlayerService {

	private MediaPlayerComponent mediaPlayerComponent;


	/**
	 * Abstract representation of a mediaplayer
	 * @author IsNull
	 *
	 */
	static class MediaPlayerComponent implements IMediaPlayerComponent {

		private EventHandlerEx<EventArgs> requestReleaseEvent = new EventHandlerEx<EventArgs>();

		/**
		 * Raised when this shared MediaPlayer component should be released by its current user
		 * @return
		 */
		@Override
		public IEvent<EventArgs> getRequestReleaseEvent() { return requestReleaseEvent; }

		/**
		 * Represents an mediaplayer visual
		 */
		@Override
		public MediaPlayerFx getSharedPlayer() { return player; }

		private MediaPlayerFx player;


		public MediaPlayerComponent(MediaPlayerFx player ){
			this.player = player;
		}

		/**
		 * Causes an event which requests that the player is freed from any usage
		 * (Removed from any visual tree)
		 */
		protected void freePlayer() {
			requestReleaseEvent.fireEvent(this, EventArgs.Empty);
		}
	}


	@Override
	public MediaPlayerComponent resolveMediaPlayer() {
		if(mediaPlayerComponent == null){
			MediaPlayerVLC vlcPlayerFX = new MediaPlayerVLC();
			mediaPlayerComponent = new MediaPlayerComponent(vlcPlayerFX);
		}else{
			mediaPlayerComponent.freePlayer();
		}
		return mediaPlayerComponent;
	}


	@Override
	public boolean isMediaPlayerAvaiable() {
		return VlcjUtil.isVlcjAvaiable();
	}
}