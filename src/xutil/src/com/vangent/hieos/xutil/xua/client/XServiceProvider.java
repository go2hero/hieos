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
package com.vangent.hieos.xutil.xua.client;

import com.vangent.hieos.xutil.template.TemplateUtil;
import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xconfig.XConfigActor;
import com.vangent.hieos.xutil.xconfig.XConfigObject;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;
import com.vangent.hieos.xutil.xml.XPathHelper;
import com.vangent.hieos.xutil.xua.utils.XUAConstants;
import com.vangent.hieos.xutil.xua.utils.XUAUtil;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.log4j.Logger;

/**
 * X-Service Provider - System providing a service that needs a X-User Assertion
 * on its service request. It has a responsibility to validate the Assertion token
 * by sending it to X-Assertion Provider. It initializes the X-ServiceProviderClient
 * which constructs the SOAP message with received token and send it to STS for
 * validation.
 * 
 * @author Fred Aabedi / Bernie Thuman
 */
public class XServiceProvider {

    private final static Logger logger = Logger.getLogger(XServiceProvider.class);
    private static XConfigActor _stsConfig = null;

    public enum Status {

        CONTINUE, ABORT
    }
    XLogMessage logMessage = null;  // For logging.

    /**
     * Constructor.
     * @param logMessage
     */
    public XServiceProvider(XLogMessage logMessage) {
        this.logMessage = logMessage;
    }

    /**
     * Run IHE XUA (Validate SAML assertion) processing rules.
     *
     * @param configActor the current configuration for the running actor.
     * @param mc messageContext contains the soap envelope.
     * @return XServiceProvider.Status  CONTINUE or ABORT
     * @throws AxisFault throws AxisFault
     */
    public XServiceProvider.Status run(XConfigActor configActor, MessageContext mc) throws AxisFault {

        // Check to see if XUA is enabled (for this actor).
        boolean xuaEnabled = configActor.isXUAEnabled();
        if (!xuaEnabled) {
            // XUA not enabled, just continue.
            return Status.CONTINUE;
        }
        logMessage.addSOAPParam("XUA:Note", "XUA is enabled!");

        // Check to see if the received soap action is an XUA constrained action or not
        boolean xuaConstrainedSOAPAction = configActor.isSOAPActionXUAEnabled(mc.getSoapAction());
        if (!xuaConstrainedSOAPAction) {
            // We are not constraining this SOAP action using XUA, just continue.
            logMessage.addSOAPParam("XUA:Note", "Skipping this SOAP Action - " + mc.getSoapAction());
            return Status.CONTINUE;
        }

        // Now check to see if this IP address should be constrained
        /*
        if (!this.IPAddressIsConstrained(mc)) {
        // Continue if we should not constain this IP address.
        logMessage.addSOAPParam("XUA:Note", "The source IP address is not constrained by XUA");
        return Status.CONTINUE;
        }*/

        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:SOAPAction", mc.getSoapAction());
        }
        // Get assertion from the received messageContext
        OMElement assertion = XServiceProvider.getSAMLAssertionFromRequest(mc);
        if (assertion == null) {
            logMessage.addErrorParam("XUA:ERROR", "No SAML Assertion found on request!");
            throw new AxisFault("XUA:Exception: No SAML Assertion found on request!");
        }
        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:SAMLAssertion", assertion.toString());
        }
        // Now validate the SAML token for validatity against the STS:

        // send the assertion for validation to STS
        boolean validationStatus = this.validateToken(XServiceProvider.getSTSConfig(), assertion);
        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:Validation_Status", validationStatus);
        }
        // Convert to a validation status (for later expansion):
        return validationStatus == true ? Status.CONTINUE : Status.ABORT;
    }

    /**
     * 
     * @return
     * @throws AxisFault
     */
    private static synchronized XConfigActor getSTSConfig() throws AxisFault {
        if (_stsConfig == null) {
            try {
                XConfig xconf;
                xconf = XConfig.getInstance();
                XConfigObject homeCommunity = xconf.getHomeCommunityConfig();
                _stsConfig = (XConfigActor) homeCommunity.getXConfigObjectWithName("sts", XConfig.STS_TYPE);
            } catch (Exception ex) {
                logger.fatal("Unable to get configuration for service", ex);
                throw new AxisFault("Unable to get configuration for service: " + ex.getMessage());
            }
        }
        return _stsConfig;
    }

    /**
     *
     * @param mc
     * @return
     */
    public String getUserNameFromRequest(MessageContext mc) {
        OMElement assertion = null;
        try {
            assertion = XServiceProvider.getSAMLAssertionFromRequest(mc);
        } catch (Exception ex) {
            // Eat this.
            logger.error("Could not get SAML Assertion", ex);
            return null;
        }
        if (assertion == null) {
            return null;
        }
        String userName = null;
        String SPProviderID = null;
        String Issuer = null;
        // Get the Issuer element from the SAML Token.
        OMElement issuerEle = assertion.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer"));
        if (issuerEle != null) {
            SPProviderID = issuerEle.getAttributeValue(new QName("SPProvidedID"));
            Issuer = issuerEle.getText();
        }
        // Get the Subject element from the SAML Token.
        OMElement subjectEle = assertion.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Subject"));
        if (subjectEle != null) {
            OMElement nameIDEle = subjectEle.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "NameID"));
            if (nameIDEle != null) {
                userName = nameIDEle.getText();
            }
        }
        return SPProviderID + "<" + userName + "@" + Issuer + ">";
    }

    /**
     *
     * @param messageContext
     * @return
     * @throws AxisFault
     */
    /*
    private boolean IPAddressIsConstrained(MessageContext messageContext) throws AxisFault {
        XUAConfig xuaConfig = XUAConfig.getInstance();
        String sourceIPAddress = this.getSourceIPAddress(messageContext);
        if (sourceIPAddress == null) {
            // If for some reason, we can not get the source IP address - constrain anyway.
            return true;
        }
        // Now see if we should constrain the IP address (based on the configuration).
        return xuaConfig.IPAddressIsConstrained(sourceIPAddress);
    }*/

    /**
     *
     * @param messageContext
     * @return
     */
    private String getSourceIPAddress(MessageContext messageContext) {
        String sourceIPAddress = null;
        try {
            sourceIPAddress = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR);
            logMessage.addSOAPParam("XUA:SourceIPAddress", sourceIPAddress);
        } catch (Exception e) {
            logMessage.addSOAPParam("XUA:SourceIPAddress", "Could not get source IP address; constraining source.");
            logger.error("XUA:Exception: Could not get source IP address; constaining source.", e);
        }
        return sourceIPAddress;
    }

    /**
     * Get the assertion from the Ws-Security header.
     *
     * @param mc messageContext, received messageContext
     * @return OMElement, assertion element
     */
    public static OMElement getSAMLAssertionFromRequest(MessageContext mc) throws AxisFault {
        SOAPEnvelope envelope = mc.getEnvelope();
        // Verify the request header is not null
        SOAPHeader requestHeader = envelope.getHeader();
        if (requestHeader == null) {
            throw new AxisFault("XUA:Exception: SOAP header should not be null.");
        }
        OMElement securityEle = requestHeader.getFirstChildWithName(new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "Security"));
        OMElement assertion = null;
        if (securityEle != null) {
            assertion = securityEle.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion"));
        }
        return assertion;
    }

    /**
     * Validate the received assertion
     * 
     * @param assertion, SAML assertion
     * @param stsUrl STS endpoint URL
     * @throws AxisFault Handling the exceptions
     * @return boolean true on success, false if SAML assertion is not validated.
     */
    public boolean validateToken(XConfigActor stsConfig, OMElement assertion) throws AxisFault {
        String assertionAsString = assertion.toString();
        String stsEndpointURL = stsConfig.getTransaction("ValidateToken").getEndpointURL();
        if (logMessage.isLogEnabled()) {
            logger.info("---- Validating the assertion againt STS (URL: " + stsEndpointURL + ") ----");
        }
        Map<String, String> templateVariableValues = new HashMap<String, String>();
        templateVariableValues.put("TOKEN", assertionAsString);
        OMElement tokenValidateRequestBody = TemplateUtil.getOMElementFromTemplate(
                XUAConstants.WS_TRUST_TOKEN_VALIDATE_REQUEST_BODY,
                templateVariableValues);

        OMElement elementHeader = this.constructWsTrustRequestHeader();
        SOAPEnvelope response = this.send(stsEndpointURL, tokenValidateRequestBody, elementHeader, XUAConstants.SOAP_ACTION_VALIDATE_TOKEN);
        if (logger.isInfoEnabled()) {
            logger.info("---- Received validation status ---");
        }
        return this.getSAMLValidationStatus(response);
    }

    /**
     * Construct Ws-Trust request header element
     *
     * @throws Exception, handling the exceptions
     */
    private OMElement constructWsTrustRequestHeader() throws AxisFault {
        Map<String, String> templateVariableValues = new HashMap<String, String>();
        // Deal with CreatedTime and ExpiredTime.
        templateVariableValues.put("CREATEDTIME", XUAUtil.getCreatedTime());
        templateVariableValues.put("EXPIREDTIME", XUAUtil.getExpireTime());
        OMElement reqHeaderElement = TemplateUtil.getOMElementFromTemplate(
                XUAConstants.WS_TRUST_TOKEN_VALIDATE_HEADER,
                templateVariableValues);
        return reqHeaderElement;
    }

    /**
     * Check to see the received validation status from STS
     * is valid or not
     * @param envelope, received SOAPEnvelope from STS
     * @return status, true or false
     * @throws Exception, handling exceptions
     */
    private boolean getSAMLValidationStatus(SOAPEnvelope envelope) throws AxisFault {
        boolean status = false;
        // Verify the response body is not null
        SOAPBody responseBody = envelope.getBody();
        if (responseBody == null) {
            throw new AxisFault("XUA:Exception: Response body should not be null.");
        }
        String validateStr = null;
        try {
            OMElement codeEle = XPathHelper.selectSingleNode(responseBody, "./ns:RequestSecurityTokenResponseCollection/ns:RequestSecurityTokenResponse/ns:Status/ns:Code[1]", "http://docs.oasis-open.org/ws-sx/ws-trust/200512");
            if (codeEle != null) {
                validateStr = codeEle.getText();
                logger.debug("*** XUA: SAML Validation Response Found = validateStr" + validateStr);
            } else {
                logger.error("*** XUA: SAML Validation Response = NULL");
            }
            if (validateStr != null) {
                if (validateStr.equalsIgnoreCase("http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid")) {
                    status = true;
                }
            }
        } catch (Exception ex) {
            throw new AxisFault("XUA:Exception: Could not get token validation response: " + ex.getMessage());
        }
        return status;
    }

    /**
     * Used to send the Token to STS
     *
     * It constructs the SOAP envelope message using Axis2 API, with request header,
     * request body, SAML assertion and with releavent SOAP action i.e
     * http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate, and it sends the
     * constructed SOAP message to specified STS endpoint URL for validation.
     *
     * @param stsUrl STS endpoint URL
     * @param requestBody , constructed WS-Trust Request
     * @param requestHeader
     * @param action SOAPAction
     * @throws Exception, handling the exceptions
     * @return responseSOAPEnvelope, responseSOAPEnvelope contains validation status
     */
    private SOAPEnvelope send(String stsUrl, OMElement request, OMElement requestHeader, String action) throws AxisFault {

        // Create empty SOAP Envelope
        SOAPSenderImpl soapSender = new SOAPSenderImpl();
        SOAPEnvelope requestSOAPEnvelope = soapSender.createEmptyEnvelope();

        // get request SOAP body from the SOAP Envelope
        SOAPBody requestSOAPBody = requestSOAPEnvelope.getBody();
        // Add bodyOMElement to SOAP Body as a child
        requestSOAPBody.addChild(request);
        SOAPHeaderBlock b;
        try {
            b = ElementHelper.toSOAPHeaderBlock(requestHeader, OMAbstractFactory.getSOAP12Factory());
        } catch (Exception ex) {
            throw new AxisFault("XUA:Exception: Error creating header block" + ex.getMessage());
        }
        requestSOAPEnvelope.getHeader().addChild(b);

        // Send SOAP envelope using SOAP sender for validation of the token
        // and get the response SOAP Envelope from STS
        SOAPEnvelope responseSOAPEnvelope = null;
        try {
            if (logMessage.isLogEnabled()) {
                logMessage.addSOAPParam("XUA:STS_URL", stsUrl);
                logMessage.addSOAPParam("XUA:STS_SOAPRequest", requestSOAPEnvelope);
            }
            responseSOAPEnvelope = soapSender.send(new java.net.URI(stsUrl), requestSOAPEnvelope, action);
            if (logMessage.isLogEnabled()) {
                logMessage.addSOAPParam("XUA:STS_SOAPResponse", responseSOAPEnvelope);
            }
        } catch (URISyntaxException ex) {
            throw new AxisFault(
                    "XUA:Exception: Could not interpret STS URL - " + ex.getMessage());
        }
        return responseSOAPEnvelope;
    }
}
