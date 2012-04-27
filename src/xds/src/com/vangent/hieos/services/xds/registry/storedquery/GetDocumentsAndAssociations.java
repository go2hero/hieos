/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2008-2009 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.services.xds.registry.storedquery;

import com.vangent.hieos.services.xds.registry.backend.BackendRegistry;
import com.vangent.hieos.xutil.exception.MetadataValidationException;
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsInternalException;
import com.vangent.hieos.xutil.metadata.structure.Metadata;
import com.vangent.hieos.xutil.metadata.structure.MetadataParser;
import com.vangent.hieos.xutil.metadata.structure.MetadataSupport;
import com.vangent.hieos.xutil.metadata.structure.SqParams;
import com.vangent.hieos.xutil.response.Response;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;
import java.util.ArrayList;

import java.util.List;
import org.apache.axiom.om.OMElement;

/**
 *
 * @author NIST (Adapted by Bernie Thuman).
 */
public class GetDocumentsAndAssociations extends StoredQuery {

    /**
     * 
     * @param params
     * @param returnLeafClass
     * @param response
     * @param logMessage
     * @param backendRegistry
     * @throws MetadataValidationException
     */
    public GetDocumentsAndAssociations(SqParams params, boolean returnLeafClass, Response response, XLogMessage logMessage, BackendRegistry backendRegistry)
            throws MetadataValidationException {
        super(params, returnLeafClass, response, logMessage, backendRegistry);

        // param name, required?, multiple?, is string?, is code?, support AND/OR, alternative
        validateQueryParam("$XDSDocumentEntryUniqueId", true, true, true, false, false, "$XDSDocumentEntryEntryUUID");
        validateQueryParam("$XDSDocumentEntryEntryUUID", true, true, true, false, false, "$XDSDocumentEntryUniqueId");
        validateQueryParam("$XDSAssociationStatus", false, true, true, false, false, (String[]) null);
        validateQueryParam("$MetadataLevel", false, false, false, false, false, (String[]) null);
        if (this.hasValidationErrors()) {
            throw new MetadataValidationException("Metadata Validation error present");
        }
    }

    /**
     *
     * @return
     * @throws XdsException
     */
    public Metadata runInternal() throws XdsException {
        Metadata metadata;
        SqParams params = this.getSqParams();
        String metadataLevel = params.getIntParm("$MetadataLevel");
        List<String> uids = params.getListParm("$XDSDocumentEntryUniqueId");
        List<String> uuids = params.getListParm("$XDSDocumentEntryEntryUUID");
        List<String> assocStatusValues = params.getListParm("$XDSAssociationStatus");
        if (assocStatusValues == null || assocStatusValues.isEmpty()) {
            // association status not specified.
            // Default association status to "Approved" if not specified.
            assocStatusValues = new ArrayList<String>();
            assocStatusValues.add(MetadataSupport.status_type_approved);
        }
        if (uids != null) {
            OMElement ele = getDocumentByUID(uids);
            metadata = MetadataParser.parseNonSubmission(ele);
        } else {
            if (uuids != null) {
                OMElement ele = getDocumentByUUID(uuids);
                metadata = MetadataParser.parseNonSubmission(ele);
            } else {
                throw new XdsInternalException("GetDocuments Stored Query: uuid not found, uid not found");
            }
        }

        // for documents in ele, get associations
        List<String> docIds;
        if (this.isReturnLeafClass()) {
            docIds = metadata.getExtrinsicObjectIds();
        } else {
            docIds = metadata.getObjectRefIds();
        }
        if (docIds.isEmpty()) {
            return metadata;
        }
        OMElement ele = getAssociations(docIds, assocStatusValues, null);
        metadata.addMetadata(ele, true);
        return metadata;
    }
}
