/*
 * Copyright 2003 - 2020 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.efaps.graphql.providers;

import java.util.HashSet;
import java.util.Set;

import org.efaps.eql.EQL;
import org.efaps.eql.builder.Selectables;
import org.efaps.graphql.ci.CIGraphQL;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

public class TypeProvider extends AbstractProvider
{

    private static final Logger LOG = LoggerFactory.getLogger(TypeProvider.class);

    public Set<GraphQLType> getTypes()
        throws EFapsException
    {
        final var ret = new HashSet<GraphQLType>();
        final var eval = EQL.builder().print()
                        .query(CIGraphQL.ObjectType)
                        .select()
                        .attribute(CIGraphQL.ObjectType.Name)
                        .evaluate();
        while (eval.next()) {
            final String name = eval.get(CIGraphQL.ObjectType.Name);
            LOG.info("Type: {}", name);
            final var objectTypeBldr = GraphQLObjectType.newObject()
                            .name(name);

            final var fieldEval = EQL.builder().print()
                            .query(CIGraphQL.FieldDefinition)
                            .where()
                            .attribute(CIGraphQL.FieldDefinition.ID)
                            .in(EQL.builder()
                                            .nestedQuery(CIGraphQL.ObjectType2FieldDefinition)
                                            .where()
                                            .attribute(CIGraphQL.ObjectType2FieldDefinition.FromID).eq(eval.inst())
                                            .up()
                                            .selectable(Selectables
                                                            .attribute(CIGraphQL.ObjectType2FieldDefinition.ToID)))
                            .select()
                            .attribute(CIGraphQL.FieldDefinition.Name, CIGraphQL.FieldDefinition.FieldType)
                            .linkfrom(CIGraphQL.FieldDefinition2ObjectType.FromLink)
                                .linkto(CIGraphQL.FieldDefinition2ObjectType.ToLink)
                                .attribute(CIGraphQL.ObjectType.Name)
                                .first().as("ObjectName")
                            .evaluate();

            while (fieldEval.next()) {
                final String fieldName = fieldEval.get(CIGraphQL.FieldDefinition.Name);
                final FieldType fieldType = fieldEval.get(CIGraphQL.FieldDefinition.FieldType);
                final String objectName = eval.get("ObjectName");
                LOG.info("    Field: {}", fieldName);
                objectTypeBldr.field(GraphQLFieldDefinition.newFieldDefinition()
                                .name(fieldName)
                                .type(evalOutputType(fieldType, objectName)));
            }
            ret.add(objectTypeBldr.build());
        }
        return ret;
    }

}