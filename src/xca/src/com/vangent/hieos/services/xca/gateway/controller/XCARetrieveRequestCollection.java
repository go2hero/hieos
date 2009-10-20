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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vangent.hieos.services.xca.gateway.controller;

import com.vangent.hieos.xutil.metadata.structure.MetadataTypes;
import com.vangent.hieos.xutil.metadata.structure.MetadataSupport;
import com.vangent.hieos.xutil.registry.RegistryUtility;
import com.vangent.hieos.xutil.response.Response;
import com.vangent.hieos.xutil.soap.Soap;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;

// Exceptions.
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsWSException;

// XConfig.
import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xconfig.XConfigHomeCommunity;
import com.vangent.hieos.xutil.xconfig.XConfigEntity;
import com.vangent.hieos.xutil.xconfig.XConfigTransaction;

// XATNA.
import com.vangent.hieos.xutil.atna.XATNALogger;

// Third-party.
import java.util.ArrayList;
import org.apache.axiom.om.OMElement;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernie Thuman
 */
public class XCARetrieveRequestCollection extends XCAAbstractRequestCollection {
    private final static Logger logger = Logger.getLogger(XCARetrieveRequestCollection.class);
    
    /**
     *
     * @param uniqueId
     * @param configEntity
     * @param isLocalRequest
     */
    public XCARetrieveRequestCollection(String uniqueId, XConfigEntity configEntity, boolean isLocalRequest) {
        super(uniqueId, configEntity, isLocalRequest);
    }

    /**
     *
     * @param response
     * @param logMessage
     * @return
     * @throws com.vangent.hieos.xutil.exception.XdsWSException
     * @throws com.vangent.hieos.xutil.exception.XdsException
     */
    public OMElement sendRequests(Response response, XLogMessage logMessage) throws XdsWSException, XdsException {
        // Get the root node.
        String ns = MetadataSupport.xdsB.getNamespaceURI();
        OMElement rootRequest = MetadataSupport.om_factory.createOMElement(new QName(ns, "RetrieveDocumentSetRequest"));

        // Now consolidate all requests to send out.
        ArrayList<XCARequest> allXCARequests = this.getRequests();
        for (XCARequest request : allXCARequests) {
            rootRequest.addChild(request.getRequest());
        }

        // Get Transaction configuration
        XConfigTransaction xconfigTxn = getXConfigTransaction();
        // Now send the requests out.
        OMElement result = this.sendTransaction(rootRequest, xconfigTxn);
        if (result != null) { // to be safe.
            logMessage.addOtherParam("Result (" + this.getEndpointURL() + ")", result);

            // Validate the response against the schema.
            try {
                RegistryUtility.schema_validate_local(result, MetadataTypes.METADATA_TYPE_RET);
            } catch (Exception e) {
                result = null;  // Ignore the response.
                response.add_error(MetadataSupport.XDSRepositoryMetadataError,
                        "Remote Gateway or Repository response did not validate against schema  [id = " +
                        this.getUniqueId() + ", endpoint = " + this.getEndpointURL() + "]",
                        e.toString(), logMessage);
            }

            if ((response != null) && this.isLocalRequest()) {
                XConfigHomeCommunity homeCommunity = XConfig.getInstance().getHomeCommunity();
                setHomeAttributeOnResult(result, homeCommunity.getHomeCommunityId());
            }
        }
        return result;
    }

    /**
     * 
     * @param result
     * @param home
     */
    private void setHomeAttributeOnResult(OMElement result, String homeCommunityId) {
        ArrayList<OMElement> docResponses = MetadataSupport.decendentsWithLocalName(result, "DocumentResponse");
        for (OMElement docResponse : docResponses) {
            // See if "HomeCommunityId" is already on request.
            OMElement hci = MetadataSupport.firstChildWithLocalName(docResponse, "HomeCommunityId");
            if (hci == null) {
                // Create one, was not in the response.
                hci = MetadataSupport.om_factory.createOMElement("HomeCommunityId", MetadataSupport.xdsB);
                docResponse.getFirstElement().insertSiblingBefore(hci);
            }
            hci.setText(homeCommunityId);  // Always, override whatever came back.

        }
    }

    /**
     *
     * @param request
     * @param endpoint
     * @param isLocalRequest
     * @return
     * @throws com.vangent.hieos.xutil.exception.XdsWSException
     * @throws com.vangent.hieos.xutil.exception.XdsException
     */
    private OMElement sendTransaction(
            OMElement request, XConfigTransaction xconfigTxn)
            throws XdsWSException, XdsException {

        String endpoint = xconfigTxn.getEndpointURL();
        boolean isAsyncTxn = xconfigTxn.isAsyncTransaction();
        String action = getAction(isAsyncTxn);
        String expectedReturnAction = getExpectedReturnAction(isAsyncTxn);

        logger.info("*** XCA action: " + action + ", expectedReturnAction: " + expectedReturnAction +
                    ", Async: " + isAsyncTxn + ", endpoint: " + endpoint + " ***");

        Soap soap = new Soap();
        soap.setAsync(isAsyncTxn);
        soap.soapCall(request, endpoint,
                true, // mtom
                true, // addressing
                true, // soap12
                action,
                expectedReturnAction);
        OMElement result = soap.getResult();  // Get the result.

        // Do ATNA auditing (after getting the result since we are only logging positive cases).
        this.performAudit(getATNATransaction(), request, endpoint, XATNALogger.OutcomeIndicator.SUCCESS);
        return result;
    }

    /**
     * This returns the endpoint URL for the local repository or a responding gateway depending upon
     * whether the request is local or not.
     * @return a String value representing the URL.
     */
    public String getEndpointURL() {
       
        return getXConfigTransaction().getEndpointURL();
    }

    /**
     * This method returns the action based on two criteria - whether the request is local
     * and whether the request is async.
     * @return a String value representing the action.
     */
    public String getAction(boolean isAsyncTxn)
    {
        // AMS - TODO - REFACTOR METHOD - Externalize or create a static map
        String action = "";
        if (this.isLocalRequest()) {
            // For XDS Affinity Domain option.
            if (isAsyncTxn)
                action = "urn:ihe:iti:2007:RetrieveDocumentSetAsync";
            else
                action = "urn:ihe:iti:2007:RetrieveDocumentSet";
        } else {
             if (isAsyncTxn)
                 action = "urn:ihe:iti:2007:CrossGatewayRetrieveAsync";
             else
                 action = "urn:ihe:iti:2007:CrossGatewayRetrieve";
        }
        return action;
    }

    /**
     * This method returns the expected return action based on two criteria - whether the request is local
     * and whether the request is async.
     * @return a String value representing the expected return action.
     */
    public String getExpectedReturnAction(boolean isAsyncTxn)
    {
        // AMS - TODO - REFACTOR METHOD - Externalize or create a static map
        String action = "";
        if (this.isLocalRequest()) {
            // For XDS Affinity Domain option.
            if (isAsyncTxn)
                action = "urn:ihe:iti:2007:RetrieveDocumentSetAsyncResponse";
            else
                action = "urn:ihe:iti:2007:RetrieveDocumentSetResponse";
        } else {
             if (isAsyncTxn)
                 action = "urn:ihe:iti:2007:CrossGatewayRetrieveAsyncResponse";
             else
                 action = "urn:ihe:iti:2007:CrossGatewayRetrieveResponse";
        }
        return action;
    }

    /**
     * This method returns a transaction configuration definition for either RetrieveDocumentSet or
     * CrossGatewayRetrieve, depending on whether the request is local or not.
     * @return XConfigTransaction.
     */
    private XConfigTransaction getXConfigTransaction()
    {
        String txnName = this.isLocalRequest() ? "RetrieveDocumentSet" : "CrossGatewayRetrieve";
        XConfigTransaction txn = this.getConfigEntity().getTransaction(txnName);
        return txn;
    }

    /**
     * This method returns an appropriate ATNA transaction type depending on whether the request is local or not.
     * @return a String representing an ATNA transaction Type
     */
    public String getATNATransaction() {
        return this.isLocalRequest() ? XATNALogger.TXN_ITI43 : XATNALogger.TXN_ITI39;
    }
}
