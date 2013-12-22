package vidada.viewsFX.mediabrowsers;

import java.net.URI;
import java.net.URISyntaxException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import org.controlsfx.control.Rating;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

import vidada.model.ServiceProvider;
import vidada.model.media.MediaItem;
import vidada.model.media.MediaType;
import vidada.model.system.ISystemService;
import vidada.viewmodel.MediaViewModel;
import vidada.viewsFX.player.IMediaPlayerService;
import vidada.viewsFX.player.IMediaPlayerService.IMediaPlayerComponent;
import vidada.viewsFX.player.MediaPlayerFx;
import vidada.viewsFX.util.AsyncImageProperty;
import vlcj.fx.IMediaPlayerBehavior;
import vlcj.fx.MediaPlayerSeekBehaviour;
import archimedesJ.events.EventArgs;
import archimedesJ.events.EventListenerEx;
import archimedesJ.images.IRawImageFactory;
import archimedesJ.images.ImageContainer;
import archimedesJ.images.viewer.IImageViewerService;
import archimedesJ.images.viewer.ISmartImage;
import archimedesJ.images.viewer.SmartImageLazy;
import archimedesJ.util.FileSupport;

/**
 * View of a single media item in the MediaBrowser
 * @author IsNull
 *
 */
public class MediaItemView extends BorderPane {

	private final IMediaPlayerService mediaPlayerService;
	private final ISystemService systemService = ServiceProvider.Resolve(ISystemService.class);

	private final StackPane primaryContent = new StackPane();
	private final AsyncImageProperty imageProperty = new AsyncImageProperty();
	private final ImagePane imagePane = new ImagePane(imageProperty);
	private final Rating rating = new Rating();
	private final Label description = new Label("<no description>");

	private final Button openButton = new Button();

	private final ContextMenu contextMenu;

	private static double dpiMultiplier = 2.0;
	private IMediaPlayerComponent player = null;

	// desired width of the thumb image panel
	// this is calculated dynamically
	private double oldThumbWidth=0;
	private double oldThumbHeight=0;

	// Current DataContext
	private MediaViewModel mediaViewModel = null;


	public MediaItemView(IMediaPlayerService mediaPlayerService){ 

		this.mediaPlayerService = mediaPlayerService;

		rating.setScaleX(1/dpiMultiplier);
		rating.setScaleY(1/dpiMultiplier);

		openButton.setText("open");


		imagePane.setFadeDuration(600);

		primaryContent.setAlignment(Pos.TOP_LEFT);
		primaryContent.getChildren().add(imagePane);

		StackPane.setMargin(openButton, new Insets(10));
		StackPane.setAlignment(openButton, Pos.CENTER_RIGHT);
		primaryContent.getChildren().add(openButton);

		// primaryContent.getChildren().add(rating);  // some crazy bug with rating rendering in stackpane!

		description.setId("description"); // style id
		description.setAlignment(Pos.CENTER_LEFT);
		description.setPadding(new Insets(10));


		this.setCenter(primaryContent);
		this.setBottom(description);

		contextMenu = createContextMenu();

		// event listener
		imagePane.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseInspectHandler);
		this.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedSelectionHandler);
		openButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseOpenHandler);


		imagePane.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number old, Number newWidth) {
				onItemImageChanged(false);
			}
		});
		imagePane.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number old, Number newHeight) {
				onItemImageChanged(false);
			}
		});

	}

	private ContextMenu createContextMenu(){

		final ContextMenu cm = new ContextMenu();

		MenuItem cmItem1 = new MenuItem("Open Folder");
		cmItem1.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				MediaItem media = mediaViewModel.getModel();
				systemService.open(media.getSource().getResourceLocation());
			}
		});

		cm.getItems().add(cmItem1);

		return cm;
	}

	/**
	 * Occurs when the user clicks on the media
	 */
	transient private final EventHandler<MouseEvent> mouseOpenHandler = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent me) {
			if(me.getButton().equals(MouseButton.PRIMARY)){
				onMediaOpenAction();
			}
		}
	};

	/**
	 * Occurs when the user clicks on the media
	 */
	transient private final EventHandler<MouseEvent> mousePrevClickedHandler = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent me) {
			if(me.getButton().equals(MouseButton.PRIMARY)){
				if(me.getClickCount() > 1){
					onMediaOpenAction();
				}
			}
		}
	};



	/**
	 * Occurs when the user clicks on the media
	 */
	transient private final EventHandler<MouseEvent> mouseInspectHandler = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent me) {
			onMediaInspectAction((float)(me.getX() / MediaItemView.this.getWidth()));
		}
	};



	/**
	 * Occurs when the user clicks on the media
	 */
	transient private final EventHandler<MouseEvent> mouseClickedSelectionHandler = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent me) {
			if(me.getButton().equals(MouseButton.PRIMARY)){
				if(me.getClickCount() == 1){
					mediaViewModel.setSelected(true);
				}
			} else if(me.getButton().equals(MouseButton.SECONDARY)){

				if(me.getClickCount() == 1){
					System.out.println("showing context menu:");
					//contextMenu.show(MediaItemView.this, me.getX(), me.getY());
					contextMenu.show(MediaItemView.this, Side.RIGHT, 0,0);
				}
			}


		}
	};

	transient private final IImageViewerService imageViewer = ServiceProvider.Resolve(IImageViewerService.class);
	transient private final IRawImageFactory imageFactory = ServiceProvider.Resolve(IRawImageFactory.class);

	/**
	 * 
	 */
	private void onMediaInspectAction(float relativePos){

		System.out.println("inspect media: " + mediaViewModel.getMediaType());
		if(mediaViewModel.getMediaType().equals(MediaType.MOVIE))
		{
			if(mediaPlayerService.isMediaPlayerAvaiable()){

				System.out.println("MediaView: Starting directplay:");

				MediaPlayerFx playerView = addMediaPlayer();

				// start playing and set initial position relative
				playerView.getMediaController().playMedia(mediaViewModel.getModel().getSource().getPath());
				playerView.getMediaController().setPosition(relativePos);

			}else {
				System.err.println("No MediaPlayer is avaible!");
			}
		}
	}

	private final static IMediaPlayerBehavior playerBehavior = new MediaPlayerSeekBehaviour();

	private MediaPlayerFx addMediaPlayer(){
		System.out.println("MediaItemView: adding MediaPlayer");

		player = mediaPlayerService.resolveMediaPlayer();
		player.getRequestReleaseEvent().add(playerReleaseListener);

		MediaPlayerFx playerView = player.getSharedPlayer();
		playerView.addBehavior(playerBehavior);

		primaryContent.getChildren().add(playerView);

		playerView.setWidth(primaryContent.getWidth());
		playerView.setHeight(primaryContent.getHeight());


		playerView.addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedListener);
		playerView.addEventHandler(MouseEvent.MOUSE_CLICKED, mousePrevClickedHandler);

		return playerView;
	}

	private synchronized void removeMediaPlayer(){

		if(player != null){

			System.out.println("MediaItemView: removing MediaPlayer");

			MediaPlayerFx playerView = player.getSharedPlayer();
			playerView.getMediaController().stop();
			player.getRequestReleaseEvent().remove(playerReleaseListener);
			primaryContent.getChildren().remove(playerView);
			playerView.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedListener);
			playerView.removeEventHandler(MouseEvent.MOUSE_CLICKED, mousePrevClickedHandler);
			player = null;
		}
	}

	private final EventListenerEx<EventArgs> playerReleaseListener = new EventListenerEx<EventArgs>() {
		@Override
		public void eventOccured(Object sender, EventArgs eventArgs) {
			removeMediaPlayer();
		}
	};

	private final EventHandler<MouseEvent> mouseExitedListener = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent me) {
			removeMediaPlayer();
		}
	};


	/**
	 * Occurs when a media is opened
	 */
	private void onMediaOpenAction(){
		if(mediaViewModel.getMediaType().equals(MediaType.IMAGE)){
			//
			// In case it is an image, show it in internal preview
			//
			ISmartImage smartImage;
			try {
				smartImage = new SmartImageLazy(imageFactory, new URI(mediaViewModel.getModel().getSource().getPath()));
				imageViewer.showImage(smartImage);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}else{

			//removeMediaPlayer();

			if(!mediaViewModel.open()){
				Action response = Dialogs.create()
						.owner(null)
						.title("Can not open Media")
						.masthead("No media source found!")
						.message("The file " + mediaViewModel.getTitle() + 
								" could not be opened!" + FileSupport.NEWLINE + mediaViewModel.getModel().getSource())
								.showWarning();
			}
		}
	}

	private void onItemImageChanged(boolean force){
		int desiredWidth = (int)imagePane.getWidth(); 
		int desiredHeight = (int)imagePane.getHeight();

		if(desiredWidth > 0 && desiredHeight > 0){

			// any change to previous setting?
			if(force || (desiredWidth != oldThumbWidth || desiredHeight != oldThumbHeight))
			{
				requestThumb(desiredWidth, desiredHeight);

				oldThumbWidth = desiredWidth;
				oldThumbHeight = desiredHeight;
			}
		}
	}

	private void requestThumb(int width, int height){
		ImageContainer container = mediaViewModel.getThumbnail(
				(int)(width*dpiMultiplier),
				(int)(height*dpiMultiplier));
		imageProperty.imageContainerProperty().set(container);
	}

	public void setDataContext(MediaViewModel mediaViewModel){
		this.mediaViewModel = mediaViewModel;
		imagePane.stopAnimation();
		if(mediaViewModel != null){
			description.setText(mediaViewModel.getFilename());
			rating.setRating(mediaViewModel.getRating());
		}else{
			description.setText("");
		}
		onItemImageChanged(true);
	}

}