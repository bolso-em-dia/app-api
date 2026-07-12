package com.mymoney.api.config;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers custom SQL functions for Hibernate.
 * Currently registers f_unaccent_lower for accent-insensitive search.
 */
public class UnaccentContributor implements MetadataBuilderContributor {

    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applySqlFunction(
                "f_unaccent_lower", new StandardSQLFunction("f_unaccent_lower", StandardBasicTypes.TEXT));
    }
}
