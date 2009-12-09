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

import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsInternalException;
import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xconfig.XConfigXUAProperties;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;
import com.vangent.hieos.xutil.xua.utils.XUAConstants;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
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
     * @param mc messageContext contains the soap envelope.
     * @return XServiceProvider.Status  CONTINUE or ABORT
     * @throws XdsException throws XdsException
     */
    public XServiceProvider.Status run(MessageContext mc) throws XdsException {
        // Get configuration.
        XConfig xconfig = null;
        try {
            xconfig = XConfig.getInstance();
        } catch (XdsInternalException ex) {
            logger.fatal("Exception while loading XConfig from XServiceProvider", ex);
            throw new XdsException(
                    "Exception while loading XConfig from XServiceProvider: " + ex.getMessage());
        }
        // Check to see if XUA is enabled
        boolean xuaEnabled = xconfig.getXUAPropertyAsBoolean(XUAConstants.XUAENABLED_PROPERTY);
        if (!xuaEnabled) {
            // XUA not enabled, just continue.
            return Status.CONTINUE;
        }

        // Check to see if the received soap action is an XUA constrained action or not
        XConfigXUAProperties xuaProperties = xconfig.getXUAConfigProperties();
        boolean xuaConstrainedSOAPAction = xuaProperties.containsSOAPAction(mc.getSoapAction());
        if (!xuaConstrainedSOAPAction) {
            // We are not constraining this SOAP action using XUA, just continue.
            return Status.CONTINUE;
        }
        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:SOAPAction", mc.getSoapAction());
        }
        // Get assertion from the received messageContext
        OMElement assertion = this.getSAMLAssertionFromRequest(mc);
        if (assertion == null) {
            logMessage.addErrorParam("XUA:ERROR", "No SAML Assertion found on request!");
            throw new XdsException("XUA:Exception: No SAML Assertion found on request!");
        }
        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:SAMLAssertion", assertion.toString());
        }
        // Now validate the SAML token for validatity against the STS:
        String stsUrl = xconfig.getXUAProperty(XUAConstants.STSURL_PROPERTY);
        String serviceUri = xconfig.getXUAProperty(XUAConstants.SERVICEURI_PROPERTY);

        // send the assertion for validation to STS
        boolean validationStatus = this.validateSAMLAssertion(stsUrl, serviceUri, assertion);
        if (logMessage.isLogEnabled()) {
            logMessage.addSOAPParam("XUA:Validation_Status", validationStatus);
        }
        // Convert to a validation status (for later expansion):
        return validationStatus == true ? Status.CONTINUE : Status.ABORT;
    }

    /**
     *
     * @param mc
     * @return
     */
    public String getUserNameFromRequest(MessageContext mc) {
        OMElement assertion = null;
        try {
            assertion = this.getSAMLAssertionFromRequest(mc);
        } catch (XdsException ex) {
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
        /*
        Iterator ite = assertion.getChildrenWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Subject"));
        while (ite.hasNext()) {
            OMElement subjectEle = (OMElement) ite.next();
            OMElement nameIDEle = subjectEle.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "NameID"));
            userName = nameIDEle.getText();
        }*/
        // concat username in a specified formate for ATNA logging.
        //alias"<"user"@"issuer">"
        return SPProviderID + "<" + userName + "@" + Issuer + ">";
    }

    /**
     * Get the assertion from the Ws-Security header.
     *
     * @param mc messageContext, received messageContext
     * @return OMElement, assertion element
     */
    private OMElement getSAMLAssertionFromRequest(MessageContext mc) throws XdsException {
        SOAPEnvelope envelope = mc.getEnvelope();
        // Verify the request header is not null
        SOAPHeader requestHeader = envelope.getHeader();
        if (requestHeader == null) {
            throw new XdsException("XUA:Exception: SOAP header should not be null.");
        }
        OMElement securityEle = requestHeader.getFirstChildWithName(new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "Security"));
        OMElement assertion = null;
        if (securityEle != null) {
            assertion = securityEle.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion"));
        }
        /*
        // Get Response Element
        Iterator ite = requestHeader.getChildrenWithName(new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
        "Security"));
        OMElement assertion = null;
        while (ite.hasNext()) {
        OMElement securityEle = (OMElement) ite.next();
        assertion = securityEle.getFirstChildWithName(new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion"));
        }*/
        return assertion;
    }

    /**
     * Validate the received assertion
     * 
     * @param assertion, SAML assertion
     * @param stsUrl STS endpoint URL
     * @throws XdsException Handling the exceptions
     * @return boolean true on success, false if SAML assertion is not validated.
     */
    public boolean validateSAMLAssertion(String stsUrl, String action, OMElement assertion) throws XdsException {
        String content = null;
        String assertionAsString = assertion.toString();
        if (logMessage.isLogEnabled()) {
            logger.info("---- Validating the assertion againt STS (URL: " + stsUrl + ") ----");
        }
        String requestContent = XUAConstants.WS_TRUST_TOKEN_VALIDATE_REQUEST_BODY;
        content = this.substituteVariables("__TOKEN__", assertionAsString, requestContent);
        SOAPEnvelope response = this.send(stsUrl, content, action);
        if (logger.isInfoEnabled()) {
            logger.info("---- Received validation status ---");
        }
        return this.getSAMLValidationStatus(response);
    }

    /**
     * Check to see the received validation status from STS
     * is valid or not
     * @param envelope, received SOAPEnvelope from STS
     * @return status, true or false
     * @throws Exception, habdling exceptions
     */
    private boolean getSAMLValidationStatus(SOAPEnvelope envelope) throws XdsException {
        boolean status = false;
        // Verify the response body is not null
        SOAPBody responseBody = envelope.getBody();
        if (responseBody == null) {
            throw new XdsException("XUA:Exception: Response body should not be null.");
        }
        String validateStr = null;
        do {
            OMElement resElement = envelope.getBody().getFirstChildWithName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                    "RequestSecurityTokenResponse"));
            if (resElement == null) {
                break;
            }
            Iterator iterator = resElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement statusEle = (OMElement) iterator.next();
                if (statusEle.getLocalName().equalsIgnoreCase("Status")) {
                    OMElement codeEle = statusEle.getFirstElement();
                    validateStr = codeEle.getText();
                    break;
                }
            }
        } while (false);

        if (validateStr != null) {
            if (validateStr.equalsIgnoreCase("http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/valid")) {
                status = true;
            }
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
     * @param action SOAPAction
     * @throws Exception, handling the exceptions
     * @return responseSOAPEnvelope, responseSOAPEnvelope contains validation status
     */
    private SOAPEnvelope send(String stsUrl, String request, String action) throws XdsException {
        //construct axis OMElement using request
        OMElement bodyOMElement;
        try {
            bodyOMElement = AXIOMUtil.stringToOM(request);
        } catch (XMLStreamException ex) {
            throw new XdsException(
                    "XUA:Exception: Could not convert STS request - " + ex.getMessage());
        }

        // Create empty SOAP Envelope
        SOAPSenderImpl soapSender = new SOAPSenderImpl();
        SOAPEnvelope requestSOAPEnvelope = soapSender.createEmptyEnvelope();

        // get request SOAP body from the SOAP Envelope
        SOAPBody requestSOAPBody = requestSOAPEnvelope.getBody();
        // Add bodyOMElement to SOAP Body as a child
        requestSOAPBody.addChild(bodyOMElement);

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
            throw new XdsException(
                    "XUA:Exception: Could not interpret STS URL - " + ex.getMessage());
        }
        return responseSOAPEnvelope;
    }

    /**
     *
     * @param varName
     * @param val
     * @param template
     * @return
     */
    private String substituteVariables(String varName, String val, String template) {
        Pattern pattern = Pattern.compile(varName, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(template);
        String newContent = matcher.replaceAll(val);
        return newContent;
    }
}