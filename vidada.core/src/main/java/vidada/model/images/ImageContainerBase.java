package vidada.model.images;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import archimedesJ.events.EventArgs;
import archimedesJ.events.EventHandlerEx;
import archimedesJ.events.IEvent;
import archimedesJ.images.IMemoryImage;
import archimedesJ.images.ImageContainer;

public class ImageContainerBase implements ImageContainer, Runnable {

	/**
	 * Delegate for a image change event
	 * @author IsNull
	 *
	 */
	public static interface ImageChangedCallback {
		/**
		 * Occurs when the image has changed
		 * @param container
		 */
		void imageChanged(ImageContainer container);
	}

	// final
	transient private final ExecutorService imageLoaderPool;
	transient private final ImageChangedCallback  changedCallback;
	transient private final Lock imageLoaderLock = new ReentrantLock();
	transient private final Object requestImageLock = new Object();

	// mutable
	transient private volatile IMemoryImage rawImage; // since we don't use any locks, this ensures visibility to another thread
	transient private Callable<IMemoryImage> imageLoader;
	transient private Future<?> imageLoaderTask;


	// Events

	private EventHandlerEx<EventArgs> ImageChangedEvent = new EventHandlerEx<EventArgs>();
	@Override
	public IEvent<EventArgs> getImageChangedEvent() { return ImageChangedEvent; }


	/**
	 * 
	 * @param imageLoaderPool The context in which the image should be loaded
	 * @param imageLoader The Image loader task (which will be called async) 
	 * @param changedCallback Callback event when the image has changed
	 */
	public ImageContainerBase(ExecutorService imageLoaderPool, Callable<IMemoryImage> imageLoader, ImageChangedCallback  changedCallback){
		this.imageLoaderPool = imageLoaderPool;
		this.imageLoader = imageLoader;
		this.changedCallback = changedCallback;
	}


	/**
	 * This method returns immediately
	 */
	@Override
	public void requestImage() {
		synchronized (requestImageLock) {
			if(isImageLoaded()) return;
			if(imageLoaderTask == null)
				imageLoaderTask = imageLoaderPool.submit(this);
			else {
				// already a task running...
			}
		}
	}

	/**
	 * Load the image in the callers thread
	 * (Thus, should be called asynchronously)
	 */
	@Override
	public void loadImageSync(){

		if(imageLoaderLock.tryLock()){
			try{
				if(imageLoader == null) return;
				if(isImageLoaded()) return;

				try {
					IMemoryImage loadedImage = imageLoader.call();
					setRawImage(loadedImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}finally{
				imageLoaderLock.unlock();
			}
		}
		// if another thread already processing this image
		// we can just skip here
	}

	/**
	 * This Callables entry point
	 * --> invokes loadImageSync
	 */
	@Override
	public void run() {
		loadImageSync();
		imageLoaderTask = null;
	}

	@Override
	public IMemoryImage getRawImage() {
		return rawImage;
	}


	@Override
	public boolean isImageLoaded() {
		return getRawImage() != null;
	}


	/**
	 * Informs this container that the current linked image is no longer valid
	 */
	@Override
	public void notifyImageChanged() {
		this.rawImage = null;
		requestImage();
	}


	protected void setRawImage(IMemoryImage rawImage){
		this.rawImage = rawImage;
		onImageChanged();
	}



	/**
	 * Occurs when the image has been loaded
	 */
	protected void onImageChanged(){
		ImageChangedEvent.fireEvent(this, EventArgs.Empty);
		if(changedCallback != null)
			changedCallback.imageChanged(this);
	}


	@Override
	public void awaitImage() {
		if(!isImageLoaded()){
			requestImage();
			Future<?> loader = imageLoaderTask;

			try {
				if(loader != null)
					loader.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

		}
	}


}
