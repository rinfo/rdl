package se.lagrummet.rinfo.integration.sparql.restlet.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Utility class with convenience methods related to handling page information. 
 * 
 * @author msher
 */
public class PageUtils {

	private static final Logger log = Logger.getLogger(PageUtils.class);


	/**
	 * Parse page parameters and arrange as a HashMap. 
	 * 
	 * @param params URL encoded (UTF-8) string with parameters, delimited by
	 * ampersand character.
	 * @return HashMap of parameter key and value pairs. Empty if no parameters
	 * could be parsed.
	 */
	public static HashMap<String, String> parsePageParameters(String params) {
		
		HashMap<String, String> map = new HashMap<String, String>();

		try {
			String urlDecoded = URLDecoder.decode(params, "UTF-8");	
			urlDecoded = StringUtils.removeStart(urlDecoded, "?");

			String[] pairs = urlDecoded.split("&");			
			for(String s : pairs) {
				String[] keyValue = s.split("=");
				if (keyValue.length == 2) {
					map.put(keyValue[0], keyValue[1]);
				}
			}			
		} catch (UnsupportedEncodingException e) {
			log.error("Could not URL decode page parameters: " + params, e);
		}
		
		return map;
	}
	
	
}
