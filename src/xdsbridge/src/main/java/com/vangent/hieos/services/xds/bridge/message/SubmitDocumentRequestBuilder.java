/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2011 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vangent.hieos.services.xds.bridge.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.vangent.hieos.hl7v3util.model.exception.ModelBuilderException;
import com.vangent.hieos.subjectmodel.CodedValue;
import com.vangent.hieos.subjectmodel.SubjectIdentifier;
import com.vangent.hieos.services.xds.bridge.mapper.DocumentTypeMapping;
import com.vangent.hieos.services.xds.bridge.model.Document;
import com.vangent.hieos.services.xds.bridge.model.Identifier;
import com.vangent.hieos.services.xds.bridge.model.SubmitDocumentRequest;
import com.vangent.hieos.services.xds.bridge.support.URIConstants;
import com.vangent.hieos.services.xds.bridge.support.XDSBridgeConfig;
import com.vangent.hieos.services.xds.bridge.utils.CodedValueUtils;
import com.vangent.hieos.services.xds.bridge.utils.SubjectIdentifierUtils;
import com.vangent.hieos.xutil.exception.XPathHelperException;
import com.vangent.hieos.xutil.exception.XdsIOException;
import com.vangent.hieos.xutil.soap.Mtom;
import com.vangent.hieos.xutil.xml.XPathHelper;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Class description
 *
 *
 * @version        v1.0, 2011-06-09
 * @author         Vangent
 */
public class SubmitDocumentRequestBuilder
        extends AbstractXdsBridgeMessageBuilder {

    /** The logger instance. */
    private static final Logger logger =
        Logger.getLogger(SubmitDocumentRequestBuilder.class);

    /**
     * Sole constructor.
     *
     *
     * @param xdsbridgeConfig xdsbridge configuration object
     */
    public SubmitDocumentRequestBuilder(XDSBridgeConfig xdsbridgeConfig) {

        super(xdsbridgeConfig);
    }

    /**
     * Method description
     *
     *
     * @param elem
     *
     * @return
     *
     * @throws ModelBuilderException
     */
    public SubmitDocumentRequest buildSubmitDocumentRequest(OMElement elem)
            throws ModelBuilderException {

        SubmitDocumentRequest result = unmarshal(elem);

        validate(result);

        return result;
    }

    /**
     * Method description
     *
     *
     *
     * @param elem
     * @param expr
     *
     * @return
     *
     * @throws IOException
     * @throws XPathHelperException
     * @throws XdsIOException
     */
    private byte[] parseBinaryContent(OMElement elem, String expr)
            throws XdsIOException, IOException, XPathHelperException {

        byte[] result = null;

        OMElement binelem = XPathHelper.selectSingleNode(elem, expr,
                                URIConstants.XDSBRIDGE_URI);

        if (binelem != null) {

            Mtom mtom = new Mtom();

            mtom.decode(binelem);

            result = mtom.getContents();

            if ((result != null) && (result.length < 1)) {
                result = null;
            }
        }

        return result;
    }

    /**
     * Method description
     *
     *
     *
     *
     * @param elem
     * @param expr
     *
     * @return
     *
     * @throws XPathHelperException
     */
    private CodedValue parseCodedValue(OMElement elem, String expr)
            throws XPathHelperException {

        CodedValue result = null;
        OMElement cvelem = XPathHelper.selectSingleNode(elem, expr,
                               URIConstants.XDSBRIDGE_URI);

        if (cvelem != null) {

            result = CodedValueUtils.parseCodedValue(cvelem);
        }

        return result;
    }

    /**
     * Method description
     *
     *
     * @param docelem
     *
     * @return
     *
     *
     * @throws IOException
     * @throws XPathHelperException
     * @throws XdsIOException
     */
    private Document parseDocument(OMElement docelem)
            throws XPathHelperException, XdsIOException, IOException {

        Document result = new Document();

        result.setId(parseText(docelem, "./ns:Id"));
        result.setReplaceId(parseText(docelem, "./ns:ReplaceId"));
        result.setType(parseCodedValue(docelem, "./ns:Type"));
        result.setConfidentialityCode(parseCodedValue(docelem,
                "./ns:ConfidentialityCode"));
        result.setContent(parseBinaryContent(docelem, "./ns:Content"));

        // use xdsbridgeconfig.xml to map and populate format
        DocumentTypeMapping mapping =
            getXdsBridgeConfig().findDocumentTypeMapping(result.getType());

        if (mapping != null) {

            result.setFormat(mapping.getFormat());
            result.setMimeType(mapping.getMimeType());
        }

        return result;
    }

    /**
     * Method description
     *
     *
     * @param elem
     * @param expr
     *
     * @return
     *
     * @throws XPathHelperException
     */
    private Identifier parseIdentifier(OMElement elem, String expr)
            throws XPathHelperException {

        Identifier result = null;
        OMElement idelem = XPathHelper.selectSingleNode(elem, expr,
                               URIConstants.XDSBRIDGE_URI);

        if (idelem != null) {

            result = new Identifier();
            result.setAssigningAuthorityName(parseText(idelem,
                    "@assigningAuthorityName"));
            result.setRoot(parseText(idelem, "@root"));
            result.setExtension(parseText(idelem, "@extension"));
        }

        return result;
    }

    /**
     * Method description
     *
     *
     * @param elem
     * @param expr
     *
     * @return
     *
     * @throws XPathHelperException
     */
    private SubjectIdentifier parseSubjectIdentifier(OMElement elem,
            String expr)
            throws XPathHelperException {

        SubjectIdentifier result = null;
        OMElement idelem = XPathHelper.selectSingleNode(elem, expr,
                               URIConstants.XDSBRIDGE_URI);

        if (idelem != null) {

            String root = parseText(idelem, "@root");
            String extension = parseText(idelem, "@extension");

            result = SubjectIdentifierUtils.createSubjectIdentifier(root,
                    extension);
        }

        return result;
    }

    /**
     * Method description
     *
     *
     *
     * @param elem
     * @param expr
     *
     * @return
     *
     * @throws XPathHelperException
     */
    private String parseText(OMElement elem, String expr)
            throws XPathHelperException {

        String result = XPathHelper.stringValueOf(elem, expr,
                            URIConstants.XDSBRIDGE_URI);

        return StringUtils.trimToNull(result);
    }

    /**
     * Method description
     *
     *
     * @param elem
     *
     * @return
     *
     * @throws ModelBuilderException
     */
    private SubmitDocumentRequest unmarshal(OMElement elem)
            throws ModelBuilderException {

        SubmitDocumentRequest result = new SubmitDocumentRequest();

        try {

            result.setPatientId(parseSubjectIdentifier(elem, "./ns:PatientId"));
            result.setOrganizationId(parseIdentifier(elem,
                    "./ns:OrganizationId"));

            List<OMElement> docelems = XPathHelper.selectNodes(elem,
                                           "./ns:Documents/ns:Document",
                                           URIConstants.XDSBRIDGE_URI);

            if (docelems != null) {

                List<Document> docs = new ArrayList<Document>();

                for (OMElement docelem : docelems) {

                    docs.add(parseDocument(docelem));
                }

                result.setDocuments(docs);
            }

        } catch (Exception e) {

            throw new ModelBuilderException(e.getMessage(), e);
        }

        return result;
    }

    /**
     * Method description
     *
     *
     * @param result
     *
     * @throws ModelBuilderException
     */
    private void validate(SubmitDocumentRequest result)
            throws ModelBuilderException {

        StringBuilder errmsg = new StringBuilder();

        // must have a pid, at least a root
        SubjectIdentifier pid = result.getPatientId();

        if ((pid == null)) {

            errmsg.append("Request must contain a PatientId.\n");

        } else {

            String pidRoot = pid.getIdentifierDomain().getUniversalId();

            if (StringUtils.isBlank(pidRoot)) {
                errmsg.append("PatientId must have a root attribute.\n");
            }
        }

        List<Document> docs = result.getDocuments();

        if ((docs == null) || docs.isEmpty()) {

            errmsg.append("Request must contain at least one document.\n");

        } else {

            for (int i = 0; i < docs.size(); ++i) {

                Document doc = docs.get(i);
                String id = doc.getId();

                if (StringUtils.isBlank(id)) {
                    id = String.format("%02d", i + 1);
                }

                CodedValue type = doc.getType();

                if (type == null) {

                    errmsg.append(
                        String.format("Document %s must have a Type.%n", id));

                } else {

                    if (StringUtils.isBlank(type.getCode())) {

                        errmsg.append(
                            String.format(
                                "Document %s must have a Type/@code.%n", id));
                    }

                    if (StringUtils.isBlank(type.getCodeSystem())) {

                        errmsg.append(
                            String.format(
                                "Document %s must have a Type/@codeSystem.%n",
                                id));
                    }

                    if (doc.getFormat() == null) {

                        String.format(
                            "Document type %s:%s is unrecognized and does not have a mapping.",
                            type.getCode(), type.getCodeSystem());
                    }

                }

                CodedValue confCode = doc.getConfidentialityCode();

                if (confCode != null) {

                    if (StringUtils.isBlank(confCode.getCode())) {

                        errmsg.append(
                            String.format(
                                "Document %s must have a ConfidentialityCode/@code%n",
                                id));
                    }

                    if (StringUtils.isBlank(confCode.getCodeSystem())) {

                        errmsg.append(
                            String.format(
                                "Document %s must have a ConfidentialityCode/@codeSystem%n",
                                id));
                    }
                }

                if (doc.getContent() == null) {

                    errmsg.append(
                        String.format(
                            "Document %s must contain content.%n", id));
                }
            }
        }

        if (errmsg.length() > 0) {
            throw new ModelBuilderException(errmsg.toString().trim());
        }
    }
}
