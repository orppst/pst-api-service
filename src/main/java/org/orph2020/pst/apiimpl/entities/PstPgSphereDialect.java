package org.orph2020.pst.apiimpl.entities;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.javastro.ivoacore.pgsphere.PgSphereDialect;

public class PstPgSphereDialect extends PgSphereDialect {
    @Override
    public void contributeTypes(TypeContributions contributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(contributions, serviceRegistry);
        contributions.contributeJavaType(DegreePointJavaType.INSTANCE);
    }
}
