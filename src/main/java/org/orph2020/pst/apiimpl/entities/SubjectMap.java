package org.orph2020.pst.apiimpl.entities;
/*
 * Created on 12/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import org.ivoa.dm.proposal.prop.Person;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table( name = "SubjectMap" )
/**
 * A mapping between AAI identities and Person in ProposalDM.
 */
public class SubjectMap {
   @Id
   @GeneratedValue(strategy = IDENTITY)
   long _id;

    @OneToOne
    @JoinColumn(name = "person_id")
    //TODO the below annotation is trying to express the fact that a whole person or just the person id is returned - this does not work https://github.com/eclipse/microprofile-open-api/blob/master/spec/src/main/asciidoc/microprofile-openapi-spec.asciidoc#filter might allow
    @org.eclipse.microprofile.openapi.annotations.media.Schema(description="the person here",oneOf = {Person.class, Integer.class},allOf = {})
    public Person person;

   public SubjectMap(Person person, String uid) {
      this.person = person;
      this.uid = uid;
   }
   public SubjectMap()
   {
   }

   public String uid;

   public Person getPerson() {
      return person;
   }

   public void setPerson(Person person) {
      this.person = person;
   }
}
