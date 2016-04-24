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
public class DBConfigReloadablePropertiesFactoryBean extends PropertiesFactoryBean implements DisposableBean {

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
		if (!isSingleton())
			throw new RuntimeException("ReloadablePropertiesFactoryBean only works as singleton");
		reloadableProperties = new DBConfigReloadablePropertiesImpl();
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

		jdbcTemplate = new JdbcTemplate(dataSource);
		log.debug("load config from db");
		String sql = "select * from config";
		List<KeyValueModel> list = jdbcTemplate.query(sql, new RowMapper<KeyValueModel>() {

			@Override
			public KeyValueModel mapRow(ResultSet rs, int rowIndex) throws SQLException {
				KeyValueModel m = new KeyValueModel();
				m.setKey(rs.getString("key"));
				m.setValue(rs.getString("value"));
				return m;
			}

		});

		for (KeyValueModel m : list) {
			props.put(m.getKey(), m.getValue());
			System.out.println("load config from db, key:" + m.getKey() + ", value:" + m.getValue());
		}

	}

	JdbcTemplate jdbcTemplate;

	private DataSource dataSource;

	class DBConfigReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {
		public void reloadConfiguration() throws Exception {
			doReload();
		}
	}

	class KeyValueModel {
		private String key;
		private String value;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
