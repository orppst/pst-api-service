package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;

@ApplicationScoped
public class XmlMapperProducer {

    @Produces
    @Singleton
    @Typed(XmlMapper.class)
    public XmlMapper xmlMapper() {
        return XmlMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultUseWrapper(false)
                .build();
    }
}
