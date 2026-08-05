package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import org.ivoa.dm.proposal.management.ProposalManagementModel;
import org.ivoa.vodml.json.VodmlHandlerInstantiator;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@ApplicationScoped
public class XmlMapperProducer {

    @Produces
    @Singleton
    @Typed(XmlMapper.class)
    public XmlMapper xmlMapper() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(utc);

        XmlMapper xmlMapper = XmlMapper.builder()
                .visibility(PropertyAccessor.FIELD, Visibility.ANY)
                .visibility(PropertyAccessor.GETTER, Visibility.NONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultTimeZone(utc)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
                .defaultDateFormat(dateFormat)
                .disable(SerializationFeature.WRAP_ROOT_VALUE)
                .disable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .defaultUseWrapper(false)
                .addModules(ObjectMapper.findModules())
                .handlerInstantiator(new VodmlHandlerInstantiator(ProposalManagementModel.modelDescription))
                .build();
        xmlMapper.setSerializationInclusion(Include.NON_NULL);
        xmlMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        return xmlMapper;
    }
}
