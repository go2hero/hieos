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
import com.vangent.hieos.xutil.xlog.client.XLogMessage;
import com.vangent.hieos.xutil.soap.Soap;
import com.vangent.hieos.xutil.metadata.structure.HomeAttribute;

// Exceptions.
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsWSException;

// XConfig.
import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xconfig.XConfigEntity;
import com.vangent.hieos.xutil.xconfig.XConfigTransaction;
import com.vangent.hieos.xutil.xconfig.XConfigHomeCommunity;

// XATNA.
import com.vangent.hieos.xutil.atna.XATNALogger;

// Third-party.
import java.util.ArrayList;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernie Thuman
 */
public class XCAQueryRequestCollection extends XCAAbstractRequestCollection {
    private final static Logger logger = Logger.getLogger(XCAQueryRequestCollection.class);

    /**
     *
     * @param uniqueId
     * @param configEntity
     * @param isLocalRequestString uniqueId, XConfigEntity configEntity, boolean isLocalRequest
     */
    public XCAQueryRequestCollection(String uniqueId, XConfigEntity configEntity, boolean isLocalRequest) {
        super(uniqueId, configEntity, isLocalRequest);
    }

    /**
     * This returns the endpoint URL for the local registry or a responding gateway depending upon
     * whether the request is local or not.
     * @return a String value representing the URL.
     */
    public String getEndpointURL() {
        return this.getXConfigTransaction().getEndpointURL();
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
        OMElement rootRequest = MetadataSupport.om_factory.createOMElement("AdhocQueryRequest", MetadataSupport.ebQns3);

        // Now consolidate all requests to send out.
        ArrayList<XCARequest> allXCARequests = this.getRequests();
        for (XCARequest request : allXCARequests) {
            rootRequest.addChild(((XCAQueryRequest) request).getResponseOption());
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
                RegistryUtility.schema_validate_local(result, MetadataTypes.METADATA_TYPE_SQ);
            } catch (Exception e) {
                result = null;  // Ignore the response.
                response.add_error(MetadataSupport.XDSRegistryMetadataError,
                        "Remote Gateway or Registry response did not validate against schema  [id = " +
                        this.getUniqueId() + ", endpoint = " + this.getEndpointURL() + "]",
                        e.toString(), logMessage);
            }

            if ((result != null) && this.isLocalRequest()) {
                XConfigHomeCommunity homeCommunity = XConfig.getInstance().getHomeCommunity();
                this.setHomeAttributeOnResult(result, homeCommunity.getHomeCommunityId());
            }
        }
        return result;
    }

    /**
     *
     * @param request
     * @param xconfigTxn
     * @return
     * @throws com.vangent.hieos.xutil.exception.XdsWSException
     * @throws com.vangent.hieos.xutil.exception.XdsException
     */
    private OMElement sendTransaction(OMElement request, XConfigTransaction xconfigTxn)
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
                false, // mtom
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
     * This method returns the action based on two criteria - whether the request is local
     * and whether it is async.
     * @return a String value representing the action.
     */
    public String getAction(boolean isAsyncTxn)
    {
        // AMS - TODO - REFACTOR METHOD - Externalize or create a static map
        String action = "";
        if (this.isLocalRequest()) {
            if (isAsyncTxn)
                action = "urn:ihe:iti:2007:RegistryStoredQueryAsync";
            else
                action = "urn:ihe:iti:2007:RegistryStoredQuery";
        } else {
             if (isAsyncTxn)
                 action = "urn:ihe:iti:2007:CrossGatewayQueryAsync";
             else
                 action = "urn:ihe:iti:2007:CrossGatewayQuery";
        }
        return action;
    }

    /**
     * This method returns the expected return action based on two criteria - whether the request is local
     * and whether it is async.
     * @return a String value representing the expected return action.
     */
    public String getExpectedReturnAction(boolean isAsyncTxn)
    {
        // AMS - TODO - REFACTOR METHOD - Externalize or create a static map
        String action = "";
        if (this.isLocalRequest()) {
            if (isAsyncTxn)
                action = "urn:ihe:iti:2007:RegistryStoredQueryAsyncResponse";
            else
                action = "urn:ihe:iti:2007:RegistryStoredQueryResponse";
        } else {
             if (isAsyncTxn)
                 action = "urn:ihe:iti:2007:CrossGatewayQueryAsyncResponse";
             else
                 action = "urn:ihe:iti:2007:CrossGatewayQueryResponse";
        }
        return action;
    }

    /**
     * This method returns a transaction configuration definition for either RegistryStoredQuery or
     * CrossGatewayQuery, depending on whether the request is local or not.
     * @return XConfigTransaction.
     */
    private XConfigTransaction getXConfigTransaction()
    {
        String txnName = this.isLocalRequest() ? "RegistryStoredQuery" : "CrossGatewayQuery";
        XConfigTransaction txn = this.getConfigEntity().getTransaction(txnName);
        return txn;
    }

    /**
     *
     * @param result
     * @param home
     */
    private void setHomeAttributeOnResult(OMElement result, String home) {
        HomeAttribute homeAtt = new HomeAttribute(home);
        homeAtt.set(result);
    }

    /**
     * This method returns an appropriate ATNA transaction type depending on whether the request is local or not.
     * @return a String representing an ATNA transaction Type
     */
    public String getATNATransaction() {
        return this.isLocalRequest() ? XATNALogger.TXN_ITI18 : XATNALogger.TXN_ITI38;
    }

}
