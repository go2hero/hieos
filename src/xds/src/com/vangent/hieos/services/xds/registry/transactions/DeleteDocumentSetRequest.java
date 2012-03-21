/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2012 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.services.xds.registry.transactions;

import com.vangent.hieos.xutil.exception.SchemaValidationException;
import com.vangent.hieos.xutil.exception.XdsInternalException;
import com.vangent.hieos.xutil.metadata.structure.MetadataSupport;
import com.vangent.hieos.xutil.metadata.structure.MetadataTypes;
import com.vangent.hieos.xutil.registry.RegistryUtility;
import com.vangent.hieos.xutil.response.RegistryResponse;
import com.vangent.hieos.xutil.services.framework.XBaseTransaction;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernie Thuman
 */
public class DeleteDocumentSetRequest extends XBaseTransaction {
    //Message context was added when trying to send audit message

    MessageContext messageContext;
    private final static Logger logger = Logger.getLogger(DeleteDocumentSetRequest.class);

    /**
     *
     * @param logMessage
     * @param messageContext
     */
    public DeleteDocumentSetRequest(XLogMessage logMessage, MessageContext messageContext) {
        this.log_message = logMessage;
        this.messageContext = messageContext;
        try {
            init(new RegistryResponse(), messageContext);
        } catch (XdsInternalException e) {
            logger.fatal(logger_exception_details(e));
        }
    }

    /**
     * 
     * @param removeObjectsRequest
     * @return
     */
    public OMElement run(OMElement removeObjectsRequest) {
        try {
            removeObjectsRequest.build();
            this.handleDeleteDocumentSetRequest(removeObjectsRequest);
        } catch (XdsInternalException e) {
            response.add_error(MetadataSupport.XDSRegistryError, "XDS Internal Error:\n " + e.getMessage(), this.getClass().getName(), log_message);
            logger.fatal(logger_exception_details(e));
        } catch (SchemaValidationException e) {
            response.add_error(MetadataSupport.XDSRegistryMetadataError, "Schema Validation Errors:\n" + e.getMessage(), this.getClass().getName(), log_message);
        }
        OMElement res = null;
        try {
            res = response.getResponse();
            this.log_response();
        } catch (XdsInternalException e) {
        }
        return res;

        // TBD: Implement
        //return submitObjectsRequest;
    }

    /**
     *
     * @param submitObjectsRequest
     * @throws XdsInternalException
     * @throws SchemaValidationException
     */
    private void handleDeleteDocumentSetRequest(OMElement submitObjectsRequest) throws XdsInternalException, SchemaValidationException {
        // Validate input message against XML schema.
        RegistryUtility.schema_validate_local(submitObjectsRequest, MetadataTypes.METADATA_TYPE_Rb);
    }
}