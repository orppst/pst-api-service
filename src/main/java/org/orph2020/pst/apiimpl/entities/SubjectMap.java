package org.orph2020.pst.apiimpl.entities;
/*
 * Created on 12/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

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
