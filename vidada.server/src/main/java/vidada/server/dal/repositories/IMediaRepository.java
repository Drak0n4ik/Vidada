package vidada.server.dal.repositories;

import java.util.Collection;
import java.util.List;

import vidada.model.media.MediaItem;
import vidada.model.media.MediaLibrary;
import vidada.model.pagination.ListPage;
import vidada.model.tags.Tag;
import vidada.server.queries.MediaExpressionQuery;
import archimedesJ.io.locations.ResourceLocation;

public interface IMediaRepository extends IRepository {


	/***************************************************************************
	 *                                                                         *
	 * Query                                                                   *
	 *                                                                         *
	 **************************************************************************/


	/**
	 * Query for all medias which match the given media-query
	 * @param qry
	 * @param dontOrder Skip ordering
	 * @return
	 */
	public abstract ListPage<MediaItem> query(MediaExpressionQuery qry, int pageIndex, final int maxPageSize);

	/**
	 * Returns all media items which are in the given libraries
	 * @param libraries
	 * @return
	 */
	public abstract List<MediaItem> query(Collection<MediaLibrary> libraries);

	/**
	 * Query for all medias which have the given Tag
	 */
	public abstract Collection<MediaItem> query(Tag tag);


	/**
	 * Query for the media with the given hash
	 * @param hash
	 * @return
	 */
	public abstract MediaItem queryByHash(String hash);

	/**
	 * Query for the first media item with the given path
	 * @param file Full file path to the media
	 * @param library The library in which this media is
	 * @return
	 */
	public MediaItem queryByPath(ResourceLocation file, MediaLibrary library);

	/**
	 * Returns all medias which are part of the given library
	 * @param library
	 * @return
	 */
	public abstract List<MediaItem> queryByLibrary(MediaLibrary library);


	public abstract List<MediaItem> getAllMedias();

	/**
	 * Returns the count of all medias
	 * @return
	 */
	public abstract int countAll();




	/***************************************************************************
	 *                                                                         *
	 * CRUD                                                                    *
	 *                                                                         *
	 **************************************************************************/

	public abstract void store(MediaItem mediadata);

	public abstract void store(Iterable<MediaItem> mediadatas);

	public abstract void update(MediaItem mediadata);

	public abstract void update(Iterable<MediaItem> mediadatas);

	public abstract void delete(MediaItem mediadata);

	public abstract void delete(Iterable<MediaItem> mediadatas);

	public abstract void removeAll();

}