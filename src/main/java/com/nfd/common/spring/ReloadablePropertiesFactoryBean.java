package com.nfd.common.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

import com.nfd.common.spring.ReloadingPropertyPlaceholderConfigurer.KeyValueModel;

/**
 * A properties factory bean that creates a reconfigurable Properties object.
 * When the Properties' reloadConfiguration method is called, and the file has
 * changed, the properties are read again from the file.
 */
public class ReloadablePropertiesFactoryBean extends PropertiesFactoryBean implements DisposableBean {

	private Log log = LogFactory.getLog(getClass());

	// copies of super class' private fields that don't have getters

	private Resource[] locations;
	private long[] lastModified;
	private boolean ignoreResourceNotFound = false;
	private String fileEncoding;
	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

	private List<ReloadablePropertiesListener> preListeners;

	@Override
	public void setLocation(Resource location) {
		setLocations(new Resource[] { location });
	}

	@Override
	public void setLocations(Resource[] locations) {
		this.locations = locations;
		lastModified = new long[locations.length];
		super.setLocations(locations);
	}

	protected Resource[] getLocations() {
		return locations;
	}

	@Override
	public void setFileEncoding(String encoding) {
		this.fileEncoding = encoding;
		super.setFileEncoding(encoding);
	}

	@Override
	public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
		this.propertiesPersister = (propertiesPersister != null ? propertiesPersister
				: new DefaultPropertiesPersister());
		super.setPropertiesPersister(this.propertiesPersister);
	}

	@Override
	public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
		this.ignoreResourceNotFound = ignoreResourceNotFound;
		super.setIgnoreResourceNotFound(ignoreResourceNotFound);
	}

	public void setListeners(List listeners) {
		// early type check, and avoid aliassing
		this.preListeners = new ArrayList<ReloadablePropertiesListener>();
		for (Object o : listeners) {
			preListeners.add((ReloadablePropertiesListener) o);
		}
	}

	private ReloadablePropertiesBase reloadableProperties;

	protected Properties createProperties() throws IOException {
		// would like to uninherit from AbstractFactoryBean (but it's final!)
		if (!isSingleton())
			throw new RuntimeException("ReloadablePropertiesFactoryBean only works as singleton");
		reloadableProperties = new ReloadablePropertiesImpl();
		if (preListeners != null)
			reloadableProperties.setListeners(preListeners);
		reload(true);
		return reloadableProperties;
	}

	public void destroy() throws Exception {
		reloadableProperties = null;
	}

	protected void reload(boolean forceReload) throws IOException {
		boolean reload = forceReload;
		for (int i = 0; i < locations.length; i++) {
			Resource location = locations[i];
			File file;
			try {
				file = location.getFile();
			} catch (IOException e) {
				// not a file resource
				continue;
			}
			try {
				long l = file.lastModified();
				if (l > lastModified[i]) {
					lastModified[i] = l;
					reload = true;
				}
			} catch (Exception e) {
				// cannot access file. assume unchanged.
				if (log.isDebugEnabled())
					log.debug("can't determine modification time of " + file + " for " + location, e);
			}
		}
		if (reload)
			doReload();
	}

	private void doReload() throws IOException {
		reloadableProperties.setProperties(mergeProperties());
	}

	/**
	 * Load properties into the given instance. Overridden to use
	 * {@link Resource#getFile} instead of {@link Resource#getInputStream}, as
	 * the latter may be have undesirable caching effects on a
	 * ServletContextResource.
	 *
	 * @param props
	 *            the Properties instance to load into
	 * @throws java.io.IOException
	 *             in case of I/O errors
	 * @see #setLocations
	 */
	@Override
	protected void loadProperties(Properties props) throws IOException {
		if (this.locations != null) {
			for (int i = 0; i < this.locations.length; i++) {
				Resource location = this.locations[i];
				if (logger.isInfoEnabled()) {
					logger.info("Loading properties file from " + location);
				}
				InputStream is = null;
				try {
					File file = null;
					try {
						file = location.getFile();
					} catch (IOException e) {
						logger.warn("Not a file resource, may not be able to reload: " + location, e);
					}
					if (file != null)
						is = new FileInputStream(file);
					else
						is = location.getInputStream();
					if (location.getFilename().endsWith(".xml")) {
						this.propertiesPersister.loadFromXml(props, is);
					} else {
						if (this.fileEncoding != null) {
							this.propertiesPersister.load(props, new InputStreamReader(is, this.fileEncoding));
						} else {
							this.propertiesPersister.load(props, is);
						}
					}
				} catch (IOException ex) {
					if (this.ignoreResourceNotFound) {
						if (logger.isWarnEnabled()) {
							logger.warn("Could not load properties from " + location + ": " + ex.getMessage());
						}
					} else {
						throw ex;
					}
				} finally {
					if (is != null) {
						is.close();
					}
				}
			}
		}
	}

	class ReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {
		public void reloadConfiguration() throws Exception {
			ReloadablePropertiesFactoryBean.this.reload(false);
		}
	}
	
}
