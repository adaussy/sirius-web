/*******************************************************************************
 * Copyright (c) 2019, 2021 Obeo.
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
package org.eclipse.sirius.web.services.documents;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.sirius.web.core.api.ErrorPayload;
import org.eclipse.sirius.web.core.api.IEditingContext;
import org.eclipse.sirius.web.core.api.IInput;
import org.eclipse.sirius.web.emf.services.EditingContext;
import org.eclipse.sirius.web.services.api.document.Document;
import org.eclipse.sirius.web.services.api.document.IDocumentService;
import org.eclipse.sirius.web.services.messages.IServicesMessageService;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeDescription;
import org.eclipse.sirius.web.spring.collaborative.api.ChangeKind;
import org.eclipse.sirius.web.spring.collaborative.api.EventHandlerResponse;
import org.eclipse.sirius.web.spring.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.web.spring.collaborative.api.Monitoring;
import org.eclipse.sirius.web.spring.collaborative.dto.DeleteDocumentInput;
import org.eclipse.sirius.web.spring.collaborative.dto.DeleteDocumentSuccessPayload;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Reducer used to delete a document.
 *
 * @author sbegaudeau
 */
@Service
public class DeleteDocumentEventHandler implements IEditingContextEventHandler {

    private final IDocumentService documentService;

    private final IServicesMessageService messageService;

    private final Counter counter;

    public DeleteDocumentEventHandler(IDocumentService documentService, IServicesMessageService messageService, MeterRegistry meterRegistry) {
        this.documentService = Objects.requireNonNull(documentService);
        this.messageService = Objects.requireNonNull(messageService);

        // @formatter:off
        this.counter = Counter.builder(Monitoring.EVENT_HANDLER)
                .tag(Monitoring.NAME, this.getClass().getSimpleName())
                .register(meterRegistry);
        // @formatter:on
    }

    @Override
    public boolean canHandle(IInput input) {
        return input instanceof DeleteDocumentInput;
    }

    @Override
    public EventHandlerResponse handle(IEditingContext editingContext, IInput input) {
        this.counter.increment();

        // @formatter:off
        var optionalEditingDomain = Optional.of(editingContext)
                .filter(EditingContext.class::isInstance)
                .map(EditingContext.class::cast)
                .map(EditingContext::getDomain);
        // @formatter:on

        if (input instanceof DeleteDocumentInput) {
            DeleteDocumentInput deleteDocumentInput = (DeleteDocumentInput) input;
            var optionalDocument = this.documentService.getDocument(deleteDocumentInput.getDocumentId());

            if (optionalEditingDomain.isPresent() && optionalDocument.isPresent()) {
                AdapterFactoryEditingDomain editingDomain = optionalEditingDomain.get();
                Document document = optionalDocument.get();

                ResourceSet resourceSet = editingDomain.getResourceSet();
                URI uri = URI.createURI(document.getId().toString());

                // @formatter:off
                List<Resource> resourcesToDelete = resourceSet.getResources().stream()
                        .filter(resource -> resource.getURI().equals(uri))
                        .collect(Collectors.toUnmodifiableList());
                resourcesToDelete.stream().forEach(resourceSet.getResources()::remove);
                // @formatter:on

                this.documentService.delete(document.getId());

                return new EventHandlerResponse(new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId()), new DeleteDocumentSuccessPayload(input.getId()));
            }
        }

        String message = this.messageService.invalidInput(input.getClass().getSimpleName(), DeleteDocumentInput.class.getSimpleName());
        return new EventHandlerResponse(new ChangeDescription(ChangeKind.NOTHING, editingContext.getId()), new ErrorPayload(input.getId(), message));
    }

}
