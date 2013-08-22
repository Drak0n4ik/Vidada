package vidada.model.security;

import java.util.List;

import vidada.data.SessionManager;
import vidada.model.ServiceProvider;
import archimedesJ.exceptions.NotSupportedException;
import archimedesJ.io.locations.Credentials;
import archimedesJ.util.Lists;

import com.db4o.ObjectContainer;
import com.db4o.query.Predicate;
import com.db4o.query.Query;

public class CredentialManager implements ICredentialManager {

	private final IPrivacyService privacyService = ServiceProvider.Resolve(IPrivacyService.class);


	@Override
	public Credentials creditalsFor(String domain)
			throws AuthenticationRequieredException {
		StoredCredentials credentials = findCredentials(domain);
		return credentials != null ? credentials.getCredentials(privacyService.getCryptoPad()) : null;
	}

	@Override
	public void storeCredentials(String domain, Credentials credentials)
			throws AuthenticationRequieredException {

		ObjectContainer db =  SessionManager.getObjectContainer();

		StoredCredentials existingCredentials = findCredentials(domain);
		if(existingCredentials != null){
			existingCredentials.setCredentials(credentials, privacyService.getCryptoPad());
			db.store(existingCredentials);
		}else {
			StoredCredentials newCredentials = new StoredCredentials(
					credentials,
					domain,
					privacyService.getCryptoPad());
			db.store(newCredentials);
		}
		db.commit();
	}

	@Override
	public boolean removeCredentials(String domain) {
		StoredCredentials credentials = findCredentials(domain);
		if(credentials != null){
			ObjectContainer db =  SessionManager.getObjectContainer();
			db.delete(credentials);
			db.commit();
			return true;
		}
		return false;
	}


	@SuppressWarnings("serial")
	private StoredCredentials findCredentials(final String domain){
		ObjectContainer db =  SessionManager.getObjectContainer();

		return Lists.getFirst((db.query(new Predicate<StoredCredentials>() {
			@Override
			public boolean match(StoredCredentials entity) {
				return entity.getDomain().equals(domain);
			}
		})));
	}

	@Override
	public List<StoredCredentials> getAllStoredCredentials()
			throws AuthenticationRequieredException {

		ObjectContainer db =  SessionManager.getObjectContainer();
		Query query = db.query();
		query.constrain(StoredCredentials.class);
		List<StoredCredentials> libs = query.execute();
		return Lists.newList(libs);
	}

	@Override
	public void clearCredentialStore() throws AuthenticationRequieredException {
		ObjectContainer db =  SessionManager.getObjectContainer();
		for (StoredCredentials credentials : getAllStoredCredentials()) {
			db.delete(credentials);
		}
	}


	@Override
	public Credentials requestAuthentication(String domain, String description, boolean useKeyStore) {

		Credentials credentials = null;
		if(useKeyStore){
			try {
				credentials = creditalsFor(domain);
			} catch (AuthenticationRequieredException e) {
				e.printStackTrace();
			}
		}

		if(credentials == null){
			// we have no saved credentials, we need to ask the user
			if(authProvider == null)
				throw new NotSupportedException("No authProvider was registered. -> requestAuthentication");

			credentials = authProvider.authenticate(domain, description);
			if(credentials != null && credentials.isRemember()){
				try {
					storeCredentials(domain, credentials);
				} catch (AuthenticationRequieredException e) {
					e.printStackTrace();
				}
			}
		}
		return credentials;
	}

	transient private AuthProvider authProvider;
	@Override
	public void register(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}


}
