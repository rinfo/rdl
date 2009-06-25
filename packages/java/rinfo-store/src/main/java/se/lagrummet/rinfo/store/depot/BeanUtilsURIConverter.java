package se.lagrummet.rinfo.store.depot;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;


public class BeanUtilsURIConverter implements Converter {

    public BeanUtilsURIConverter() {}

    public Object convert(Class type, Object value) {
        try {
            return new URI(value.toString());
        } catch (URISyntaxException e) {
            throw new ConversionException(e);
        }
    }

    public static void registerIfNoURIConverterIsRegistered() {
        if (ConvertUtils.lookup(URI.class) == null) {
            register();
        }
    }

    public static void register() {
        ConvertUtils.register(new BeanUtilsURIConverter(), URI.class);
    }

}
