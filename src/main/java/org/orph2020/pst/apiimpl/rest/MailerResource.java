package org.orph2020.pst.apiimpl.rest;

import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/*
    Temporary class to demonstrate sending an email to a hardcoded address using a Qute Template
 */

@Path("mailer")
public class MailerResource {

    @CheckedTemplate
    static class Templates {
        public static native MailTemplate.MailTemplateInstance test(String name);
    }

    @GET
    public Uni<Void> send() {
        return Templates.test("Darren")
                .to("darren.walker@manchester.ac.uk")
                .subject("Testing Qute")
                .send();
    }
}
