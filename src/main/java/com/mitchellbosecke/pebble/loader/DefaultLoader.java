/*******************************************************************************
 * This file is part of Pebble.
 * 
 * Original work Copyright (c) 2009-2013 by the Twig Team
 * Modified work Copyright (c) 2013 by Mitchell Bösecke
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 ******************************************************************************/
package com.mitchellbosecke.pebble.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mitchellbosecke.pebble.error.LoaderException;

public class DefaultLoader implements Loader {

	private static final Logger logger = LoggerFactory.getLogger(DefaultLoader.class);

	private String prefix;

	private String suffix;

	private String charset = "UTF-8";

	@Override
	public Reader getReader(String templateName) throws LoaderException {

		InputStreamReader isr = null;
		Reader reader = null;

		InputStream is = null;

		StringBuilder path = new StringBuilder("");
		if (getPrefix() != null) {

			path.append(getPrefix());

			if (!getPrefix().endsWith(String.valueOf(File.separatorChar))) {
				path.append(File.separatorChar);
			}
		}

		String location = path.toString() + templateName + (getSuffix() == null ? "" : getSuffix());
		logger.debug("Looking for template in {}.", location);

		// try ContextClassLoader
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		is = ccl.getResourceAsStream(location);

		// try ResourceLoader's class loader
		ClassLoader rcl = DefaultLoader.class.getClassLoader();
		if (is == null) {
			is = rcl.getResourceAsStream(location);
		}

		/*
		 * if template name contains path segments, move those segments into the
		 * path variable. The below technique needs to know the difference
		 * between the path and file name.
		 */
		String[] pathSegments = templateName.split("\\\\|/");

		if (pathSegments.length > 1) {
			// file name is the last segment
			templateName = pathSegments[pathSegments.length - 1];
		}
		for (int i = 0; i < (pathSegments.length - 1); i++) {
			path.append(pathSegments[i]).append(File.separatorChar);
		}

		// try to load File
		if (is == null) {
			File file = new File(path.toString(), templateName);
			if (file.exists() && file.isFile()) {
				try {
					is = new FileInputStream(file);
				} catch (FileNotFoundException e) {
				}
			}
		}

		if (is == null) {
			throw new LoaderException(null, "Could not find template \"" + location + "\"");
		}

		try {
			isr = new InputStreamReader(is, charset);
			reader = new BufferedReader(isr);
		} catch (UnsupportedEncodingException e) {
		}

		return reader;
	}

	public String getSuffix() {
		return suffix;
	}

	@Override
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getPrefix() {
		return prefix;
	}

	@Override
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getCharset() {
		return charset;
	}

	@Override
	public void setCharset(String charset) {
		this.charset = charset;
	}
}