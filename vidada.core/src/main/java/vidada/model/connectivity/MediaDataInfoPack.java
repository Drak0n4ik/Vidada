package vidada.model.connectivity;

import java.util.List;

/***
 * Represents a collection of media data information
 * for exchange and easy serialization
 * 
 * @author IsNull
 *
 */
public class MediaDataInfoPack {


	public MediaDataInfoPack(){}

	public MediaDataInfoPack(List<MediaDataInfo> medias)
	{
		this.Medias = medias;
	}


	/**
	 * Protocol version
	 */
	public String version = "1.0";

	/**
	 * 
	 */
	public List<MediaDataInfo> Medias;
}
