/*******************************************************************************
 * Copyright (c) 2021 THALES GLOBAL SERVICES.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.graphql.datafetchers.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

import org.eclipse.sirius.web.annotations.graphql.GraphQLMutationTypes;
import org.eclipse.sirius.web.annotations.spring.graphql.MutationDataFetcher;
import org.eclipse.sirius.web.core.api.ErrorPayload;
import org.eclipse.sirius.web.core.api.IPayload;
import org.eclipse.sirius.web.graphql.messages.IGraphQLMessageService;
import org.eclipse.sirius.web.graphql.schema.MutationTypeProvider;
import org.eclipse.sirius.web.spring.collaborative.api.IEditingContextEventProcessorRegistry;
import org.eclipse.sirius.web.spring.collaborative.diagrams.dto.UpdateNodeBoundsInput;
import org.eclipse.sirius.web.spring.collaborative.diagrams.dto.UpdateNodeBoundsSuccessPayload;
import org.eclipse.sirius.web.spring.graphql.api.IDataFetcherWithFieldCoordinates;

import graphql.schema.DataFetchingEnvironment;

/**
 * The data fetcher used to move or resize a node on a diagram
 * <p>
 * It will be used to handle the following GraphQL field:
 * </p>
 *
 * <pre>
 * type Mutation {
 *   updateNodeBounds(input: UpdateNodeBoundsInput!): UpdateNodeBoundsPayload!
 * }
 * </pre>
 *
 * @author fbarbin
 */
// @formatter:off
@GraphQLMutationTypes(
    input = UpdateNodeBoundsInput.class,
    payloads = {
        UpdateNodeBoundsSuccessPayload.class
    }
)
@MutationDataFetcher(type = MutationTypeProvider.TYPE, field = MutationUpdateNodeBoundsDataFetcher.UPDATE_NODE_BOUNDS_FIELD)
// @formatter:on
public class MutationUpdateNodeBoundsDataFetcher implements IDataFetcherWithFieldCoordinates<IPayload> {

    public static final String UPDATE_NODE_BOUNDS_FIELD = "updateNodeBounds"; //$NON-NLS-1$

    private final ObjectMapper objectMapper;

    private final IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry;

    private final IGraphQLMessageService messageService;

    public MutationUpdateNodeBoundsDataFetcher(ObjectMapper objectMapper, IEditingContextEventProcessorRegistry editingContextEventProcessorRegistry, IGraphQLMessageService messageService) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.editingContextEventProcessorRegistry = Objects.requireNonNull(editingContextEventProcessorRegistry);
        this.messageService = Objects.requireNonNull(messageService);
    }

    @Override
    public IPayload get(DataFetchingEnvironment environment) throws Exception {
        Object argument = environment.getArgument(MutationTypeProvider.INPUT_ARGUMENT);
        var input = this.objectMapper.convertValue(argument, UpdateNodeBoundsInput.class);

        // @formatter:off
        return this.editingContextEventProcessorRegistry.dispatchEvent(input.getEditingContextId(), input)
                .orElse(new ErrorPayload(input.getId(), this.messageService.unexpectedError()));
        // @formatter:on
    }

}
