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
package com.vangent.hieos.xtest.validator;

import com.vangent.hieos.xutil.exception.MetadataValidationException;
import com.vangent.hieos.xutil.exception.MetadataException;
import com.vangent.hieos.xutil.exception.XdsInternalException;
import com.vangent.hieos.xutil.metadata.structure.Metadata;
import com.vangent.hieos.xutil.metadata.structure.MetadataSupport;

import com.vangent.hieos.xutil.xml.XMLParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;

/**
 *
 * @author thumbe
 */
public class TestValidator {

    Metadata m;
    StringBuffer errs = new StringBuffer();
    boolean error = false;
    OMElement test_assertions;

    /**
     *
     * @param m
     */
    public TestValidator(Metadata m) {
        this.m = m;
    }

    /**
     *
     * @param test_assertions
     */
    public TestValidator(OMElement test_assertions) {
        this.test_assertions = test_assertions;
    }

    /**
     *
     * @param test_assertion_file
     * @param subset_name
     * @throws XdsInternalException
     */
    public TestValidator(File test_assertion_file, String subset_name) throws XdsInternalException {
        test_assertions = XMLParser.fileToOM(test_assertion_file);
        if (subset_name != null) {
            test_assertions = MetadataSupport.firstChildWithLocalName(test_assertions, subset_name);
            if (test_assertions == null) {
                throw new XdsInternalException("Validator: assertion subset " + subset_name + " not found in file " + test_assertion_file);
            }
        }
    }

    // return is ArrayList of ArrayLists.  Each internal ArrayList has two elements, The first is testname, the second is status, third
    // is errors
    /**
     *
     * @return
     * @throws MetadataException
     */
    public ArrayList<ArrayList> getPatterns() throws MetadataException {
        ArrayList<ArrayList> patterns = new ArrayList<ArrayList>();

        clearError();
        ArrayList<String> stat = buildStatus("hasSubmissionSet", hasSubmissionSet());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("ssApproved", ssApproved());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("docsApproved", docsApproved());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("ss1Doc", ss1Doc());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("ss2Doc", ss2Doc());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("sswithOneFol", sswithOneFol());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("sswithOneDocOneFol", sswithOneDocOneFol());
        if (stat != null) {
            patterns.add(stat);
        }

        clearError();
        stat = buildStatus("sswithTwoDocOneFolOneDocInFol", sswithTwoDocOneFolOneDocInFol());
        if (stat != null) {
            patterns.add(stat);
        }

        return patterns;
    }

    ArrayList<String> mkArrayList(String s1, String s2, String s3) {
        ArrayList<String> al = new ArrayList<String>();
        al.add(s1);
        al.add(s2);
        al.add(s3);
        return al;
    }

    ArrayList<String> buildStatus(String testname, boolean status) {
        if (status) {
            return mkArrayList(testname, "true", null);
        }
        return null;
//		String errs = getErrors();
//		if (errs == null || errs.equals("")) return mkArrayList(testname, "false", null);
//		return mkArrayList(testname, "false", errs);
    }

    void err(String msg) {
        errs.append("Validator: ");
        errs.append(msg);
        errs.append('\n');
        error = true;
    }

    void clearError() {
        error = false;
        errs = new StringBuffer();
    }

    /**
     *
     * @return
     */
    public boolean hasError() {
        return error;
    }

    /**
     *
     * @return
     */
    public String getErrors() {
        return errs.toString();
    }

    /**
     *
     * @param count
     * @return
     * @throws XdsInternalException
     */
    public boolean hasObjectRefs(int count) throws XdsInternalException {
        int originalSize = m.getAllObjects().size();
        if (originalSize != count) {
            err("Found " + originalSize + " ObjectRefs instead of " + count);
            return false;
        }
        return true;
    }

    /**
     *
     * @return
     */
    public boolean hasSubmissionSet() {
        if (m.getSubmissionSet() == null) {
            err("No Submission Set found");
            return false;
        }
        return true;
    }

    /**
     *
     * @return
     */
    public boolean hasNoSubmissionSet() {
        if (m.getSubmissionSet() != null) {
            err("Submission Set found");
            return false;
        }
        return true;
    }

    /**
     *
     * @param count
     * @return
     */
    public boolean hasDocuments(int count) {
        if (m.getExtrinsicObjects().size() != count) {
            err("Found " + m.getExtrinsicObjects().size() + " Documents instead of " + count);
            return false;
        }
        return true;
    }

    /**
     *
     * @param count
     * @return
     */
    public boolean hasFolders(int count) {
        if (m.getFolders().size() != count) {
            err("Found " + m.getFolders().size() + " Folders instead of " + count);
            return false;
        }
        return true;
    }

    /**
     *
     * @param count
     * @return
     */
    public boolean hasSubmissionSets(int count) {
        if (m.getSubmissionSets().size() != count) {
            err("Found " + m.getSubmissionSets().size() + " SubmissionSets instead of " + count);
            return false;
        }
        return true;
    }

    /**
     *
     * @param count
     * @return
     */
    public boolean hasAssociations(int count) {
        if (m.getAssociations().size() != count) {
            err("Found " + m.getAssociations().size() + " Associations instead of " + count);
            return false;
        }
        return true;
    }

    String att_val(OMElement ele, QName att_name) {
        String val = ele.getAttributeValue(att_name);
        if (val == null) {
            val = "";
        }
        return val;
    }

    /**
     *
     * @param source
     * @param target
     * @param type
     * @return
     * @throws MetadataException
     */
    public OMElement hasAssociation(OMElement source, OMElement target, String type) throws MetadataException {
        String source_id = att_val(source, MetadataSupport.id_qname);
        String target_id = att_val(target, MetadataSupport.id_qname);

        List<OMElement> asss = m.getAssociations();
        for (int i = 0; i < asss.size(); i++) {
            OMElement a = asss.get(i);
            if (!att_val(a, MetadataSupport.source_object_qname).equals(source_id)) {
                continue;
            }
            if (!att_val(a, MetadataSupport.target_object_qname).equals(target_id)) {
                continue;
            }
            if (!att_val(a, MetadataSupport.association_type_qname).equals(type)) {
                continue;
            }

            return a;
        }
        err(type + " assocation missing between sourceObject "
                + m.getIdentifyingString(source_id)
                + " and targetObject "
                + m.getIdentifyingString(target_id)
                + " expected " + type);
        return null;
    }

    /**
     *
     * @param source
     * @param targets
     * @param type
     * @return
     * @throws MetadataException
     */
    public OMElement hasAssociationWithOneTarget(OMElement source, List<OMElement> targets, String type) throws MetadataException {
        String source_id = att_val(source, MetadataSupport.id_qname);
        List<String> target_ids = new ArrayList<String>();
        for (OMElement tgt : targets) {
            String id = att_val(tgt, MetadataSupport.id_qname);
            target_ids.add(id);
        }

        List<OMElement> asss = m.getAssociations();
        for (int i = 0; i < asss.size(); i++) {
            OMElement a = asss.get(i);
            if (!att_val(a, MetadataSupport.source_object_qname).equals(source_id)) {
                continue; //error
            }
            boolean found_target = false;
            for (String id : target_ids) {
                if (att_val(a, MetadataSupport.target_object_qname).equals(id)) {
                    found_target = true;
                }
            }
            if (!found_target) {
                continue; //error
            }
            if (!att_val(a, MetadataSupport.association_type_qname).equals(type)) {
                continue; // error
            }
            return a;
        }
        err(type + " assocation missing between sourceObject "
                + m.getIdentifyingString(source_id)
                + " and targetObject (one of) "
                + getIdentifyingStrings(target_ids)
                + " expected " + type);
        return null;
    }

    /**
     *
     * @param target_ids
     * @return
     * @throws MetadataException
     */
    private List<String> getIdentifyingStrings(List<String> target_ids) throws MetadataException {
        List<String> idents = new ArrayList<String>();
        for (String id : target_ids) {
            idents.add(m.getIdentifyingString(id));
        }
        return idents;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean ss1Doc() throws MetadataException {
        this.hasSubmissionSet();
        this.hasDocuments(1);
        this.hasAssociations(1);
        if (this.hasError()) {
            return false;
        }

        if (this.hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member) != null) {
            return true;
        }
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean ss2Doc() throws MetadataException {
        this.hasSubmissionSet();
        this.hasDocuments(2);
        this.hasAssociations(2);
        if (this.hasError()) {
            return false;
        }

        if (this.hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member) != null) {
            return false;
        }
        if (this.hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(1), MetadataSupport.xdsB_eb_assoc_type_has_member) != null) {
            return false;
        }
        return true;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean hasRplc() throws MetadataException {
        String docUuid = null;
        boolean found = false;

        for (OMElement a : m.getAssociations()) {
            String type = m.getAssocType(a);
            if (MetadataSupport.xdsB_ihe_assoc_type_rplc.equals(type)) {
                found = true;
                if (docUuid == null) {
                    docUuid = m.getAssocTarget(a);
                } else {
                    if (docUuid.equals(m.getAssocTarget(a))) {
                        throw new MetadataException("HasRPLC test: multiple RPLC associations found for same Document");
                    }
                }
            }
        }

        return found;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean docsApproved() throws MetadataException {
        boolean stat = false;
        for (OMElement eo : m.getExtrinsicObjects()) {
            String status = eo.getAttributeValue(MetadataSupport.status_qname);
            if (status == null || !status.equals(MetadataSupport.status_type_approved)) {
                err("ExtrinsicObject " + eo.getAttributeValue(MetadataSupport.id_qname) + " has status " + status + " instead of 'Approved'");
                return false;
            }
            stat = true;
        }
        return stat;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneFolderDeprecated() throws MetadataException {
        int count = 0;
        for (OMElement fol : m.getFolders()) {
            String status = fol.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_deprecated)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Approved Folders instead of one");
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneFolderApproved() throws MetadataException {
        int count = 0;
        for (OMElement fol : m.getFolders()) {
            String status = fol.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_approved)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Approved Folders instead of one");
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneAssocDeprecated() throws MetadataException {
        int count = 0;
        for (OMElement assoc : m.getAssociations()) {
            String status = assoc.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_deprecated)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Deprecated Associations instead of one");
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneAssocApproved() throws MetadataException {
        int count = 0;
        for (OMElement assoc : m.getAssociations()) {
            String status = assoc.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_approved)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Approved Associations instead of one");
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean folsApproved() throws MetadataException {
        for (OMElement fol : m.getFolders()) {
            String status = fol.getAttributeValue(MetadataSupport.status_qname);
            if (status == null || !status.equals(MetadataSupport.status_type_approved)) {
                err("Folder " + fol.getAttributeValue(MetadataSupport.id_qname) + " has status " + status + " instead of 'Approved'");
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean ssApproved() throws MetadataException {
        OMElement ss = m.getSubmissionSet();
        if (ss == null) {
            err("No Submission Set");
            return false;
        }
        String status = ss.getAttributeValue(MetadataSupport.status_qname);
        if (status == null || !status.equals(MetadataSupport.status_type_approved)) {
            err("SubmissionSet " + ss.getAttributeValue(MetadataSupport.id_qname) + " has status " + status + " instead of 'Approved'");
            return false;
        }
        return true;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean docsDeprecated() throws MetadataException {
        for (OMElement eo : m.getExtrinsicObjects()) {
            String status = eo.getAttributeValue(MetadataSupport.status_qname);
            if (!status.equals(MetadataSupport.status_type_deprecated)) {
                err("ExtrinsicObject " + eo.getAttributeValue(MetadataSupport.id_qname) + " has status " + status + " instead of 'Deprecated'");
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneDocDeprecated() throws MetadataException {
        int count = 0;
        for (OMElement eo : m.getExtrinsicObjects()) {
            String status = eo.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_deprecated)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Deprecated ExtrinsicObjects instead of one");
        return false;
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean oneDocApproved() throws MetadataException {
        int count = 0;
        for (OMElement eo : m.getExtrinsicObjects()) {
            String status = eo.getAttributeValue(MetadataSupport.status_qname);
            if (status.equals(MetadataSupport.status_type_approved)) {
                count++;
            }
        }
        if (count == 1) {
            return true;
        }
        err("Found " + count + " Approved ExtrinsicObjects instead of one");
        return false;
    }

    /**
     *
     * @param xml
     * @throws MetadataException
     * @throws XdsInternalException
     * @throws MetadataValidationException
     */
    public void run_test_assertions(OMElement xml) throws MetadataException, XdsInternalException, MetadataValidationException {
        Metadata m = new Metadata(xml);

        run_test_assertions(m);
    }

    /**
     *
     * @param m
     * @throws MetadataException
     * @throws XdsInternalException
     */
    public void run_test_assertions(Metadata m) throws MetadataException, XdsInternalException {
        this.m = m;

        for (Iterator it = test_assertions.getChildElements(); it.hasNext();) {
            OMElement ec = (OMElement) it.next();
            String ec_name = ec.getLocalName();

            if (ec_name.equals("SSwithOneDoc")) {
                ss1Doc();
                ssApproved();
            } else if (ec_name.equals("SSApproved")) {
                ssApproved();
            } else if (ec_name.equals("DocDep")) {
                docsDeprecated();
            } else if (ec_name.equals("DocApp")) {
                docsApproved();
            } else if (ec_name.equals("HasRPLC")) {
                hasRplc();
            } else if (ec_name.equals("OneDocDep")) {
                oneDocDeprecated();
            } else if (ec_name.equals("OneDocApp")) {
                oneDocApproved();
            } else if (ec_name.equals("OneFolDep")) {
                oneFolderDeprecated();
            } else if (ec_name.equals("OneFolApp")) {
                oneFolderApproved();
             } else if (ec_name.equals("OneAssocDep")) {
                oneAssocDeprecated();
            } else if (ec_name.equals("OneAssocApp")) {
                oneAssocApproved();
            } else if (ec_name.equals("FolApp")) {
                folsApproved();
            } else if (ec_name.equals("SSwithTwoDoc")) {
                ss2Doc();
                ssApproved();
            } else if (ec_name.equals("SSwithOneDocOneFol")) {
                sswithOneDocOneFol();
            } else if (ec_name.equals("SSwithTwoDocOneFol")) {
                sswithTwoDocOneFol();
            } else if (ec_name.equals("SSwithTwoDocOneFolOneDocInFol")) {
                sswithTwoDocOneFolOneDocInFol();
            } else if (ec_name.equals("SSwithOneFol")) {
                sswithOneFol();
            } else if (ec_name.equals("None")) {
                hasNoSubmissionSet();
                hasDocuments(0);
                hasFolders(0);
                if (hasError()) {
                    continue;
                }
                hasAssociations(0);
            } else if (ec_name.equals("ObjectRefs")) {
                int count = Integer.parseInt(ec.getAttributeValue(new QName("count")));
                hasObjectRefs(count);
                hasNoSubmissionSet();
                hasDocuments(0);
                hasFolders(0);
                hasAssociations(0);
            } else if (ec_name.equals("Documents")) {
                int count = Integer.parseInt(ec.getAttributeValue(new QName("count")));
                hasDocuments(count);
            } else if (ec_name.equals("Folders")) {
                int count = Integer.parseInt(ec.getAttributeValue(new QName("count")));
                hasFolders(count);
            } else if (ec_name.equals("SubmissionSets")) {
                int count = Integer.parseInt(ec.getAttributeValue(new QName("count")));
                hasSubmissionSets(count);
            } else if (ec_name.equals("Associations")) {
                int count = Integer.parseInt(ec.getAttributeValue(new QName("count")));
                hasAssociations(count);
            } else {
                throw new XdsInternalException("QueryTransaction: validate_expected_contents(): don't understand verification request " + ec_name);
            }

        }
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean sswithOneFol() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(0);
        hasFolders(1);
        if (!hasError()) {
            hasAssociations(1);
            hasAssociation(m.getSubmissionSet(), m.getFolder(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
        }
        return !hasError();
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    /*
    public boolean addDocToExistingFolder() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(1);
        hasFolders(0);
        if (!hasError()) {
            hasAssociations(3);
            OMElement ssA = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            String ssId = m.getId(m.getSubmissionSet());

            ArrayList<OMElement> otherA = (ArrayList<OMElement>) m.getAssociations().clone();
            otherA.remove(ssA);

            OMElement doc = m.getExtrinsicObject(0);
            String docId = m.getId(doc);
            OMElement assocA = otherA.get(0);
            OMElement assocB = otherA.get(1);

            OMElement ssAssocAssoc = null;
            OMElement folDocAssoc = null;

            if (m.getSourceObject(assocA).equals(ssId)) {
                ssAssocAssoc = assocA;
                folDocAssoc = assocB;
            } else {
                ssAssocAssoc = assocB;
                folDocAssoc = assocA;
            }

            if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssAssocAssoc))
                    || !m.getSourceObject(ssAssocAssoc).equals(ssId)
                    || !m.getTargetObject(ssAssocAssoc).equals(m.getId(folDocAssoc))) {
                String msg = "";
                if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssAssocAssoc))) {
                    msg = "association type";
                }
                if (!m.getSourceObject(ssAssocAssoc).equals(ssId)) {
                    msg = msg + ", sourceObject";
                }
                if (!m.getTargetObject(ssAssocAssoc).equals(m.getId(folDocAssoc))) {
                    msg = msg + ", targetObject is not the Folder-Document association";
                }
                err("A HasMember association must link the SubmissionSet and the new Folder-to-Document Association (" + msg + ")");
            }

            if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(folDocAssoc))
                    || !m.getTargetObject(folDocAssoc).equals(docId)) {
                err("A HasMember association must link the existing folder and the new Document");
            }

        }
        return !hasError();
    }*/

    /**
     *
     * @return
     * @throws MetadataException
     */
    /*
    public boolean addExistingDocToExistingFolder() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(0);
        hasFolders(0);
        if (!hasError()) {
            hasAssociations(2);

            String ssId = m.getId(m.getSubmissionSet());

            ArrayList<OMElement> otherA = (ArrayList<OMElement>) m.getAssociations();

            OMElement assocA = otherA.get(0);
            OMElement assocB = otherA.get(1);

            OMElement ssAssocAssoc = null;
            OMElement folDocAssoc = null;

            if (m.getSourceObject(assocA).equals(ssId)) {
                ssAssocAssoc = assocA;
                folDocAssoc = assocB;
            } else {
                ssAssocAssoc = assocB;
                folDocAssoc = assocA;
            }

            if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssAssocAssoc))
                    || !m.getSourceObject(ssAssocAssoc).equals(ssId)
                    || !m.getTargetObject(ssAssocAssoc).equals(m.getId(folDocAssoc))) {
                String msg = "";
                if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssAssocAssoc))) {
                    msg = "association type";
                }
                if (!m.getSourceObject(ssAssocAssoc).equals(ssId)) {
                    msg = msg + ", sourceObject";
                }
                if (!m.getTargetObject(ssAssocAssoc).equals(m.getId(folDocAssoc))) {
                    msg = msg + ", targetObject is not the Folder-Document association";
                }
                err("A HasMember association must link the SubmissionSet and the new Folder-to-Document Association (" + msg + ")");
            }

            if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(folDocAssoc))) {
                err("A HasMember association must link the existing folder and the existing Document");
            }

        }
        return !hasError();
    }*/

    /**
     *
     * @return
     * @throws MetadataException
     */
    /*
    public boolean replaceDocument() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(1);
        hasFolders(0);
        if (!hasError()) {
            hasAssociations(2);

            String ssId = m.getId(m.getSubmissionSet());
            String docId = m.getId(m.getExtrinsicObject(0));

            ArrayList<OMElement> otherA = (ArrayList<OMElement>) m.getAssociations();

            OMElement assocA = otherA.get(0);
            OMElement assocB = otherA.get(1);

            OMElement ssDocAssoc = null;
            OMElement rplcAssoc = null;

            if (m.getSourceObject(assocA).equals(ssId)) {
                ssDocAssoc = assocA;
                rplcAssoc = assocB;
            } else {
                ssDocAssoc = assocB;
                rplcAssoc = assocA;
            }

            if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssDocAssoc))
                    || !m.getSourceObject(ssDocAssoc).equals(ssId)
                    || !m.getTargetObject(ssDocAssoc).equals(docId)) {
                String msg = "";
                if (!MetadataSupport.xdsB_eb_assoc_type_has_member.equals(m.getAssocType(ssDocAssoc))) {
                    msg = "association type";
                }
                if (!m.getSourceObject(ssDocAssoc).equals(ssId)) {
                    msg = msg + ", sourceObject";
                }
                if (!m.getTargetObject(ssDocAssoc).equals(docId)) {
                    msg = msg + ", targetObject is not the submitted document";
                }
                err("A HasMember association must link the SubmissionSet and the new Document (" + msg + ")");
            }

            if (!MetadataSupport.xdsB_ihe_assoc_type_rplc.equals(m.getAssocType(rplcAssoc))
                    || !m.getSourceObject(rplcAssoc).equals(docId)) {
                err("A RPLC association must link the submitted Document and the existing Document");
            }

        }
        return !hasError();
    }*/

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean sswithOneDocOneFol() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(1);
        hasFolders(1);
        if (!hasError()) {
            hasAssociations(4);
            List<OMElement> unknownAssocs = m.getAssociations();
            List<OMElement> folderDocAssocs = new ArrayList<OMElement>();
            OMElement a;

            a = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociation(m.getSubmissionSet(), m.getFolder(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }


            a = hasAssociation(m.getFolder(0), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
                folderDocAssocs.add(a);
            }

            a = hasAssociationWithOneTarget(m.getSubmissionSet(), folderDocAssocs, MetadataSupport.xdsB_eb_assoc_type_has_member);

        }
        return !hasError();
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean sswithTwoDocOneFol() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(2);
        hasFolders(1);
        if (!hasError()) {
            hasAssociations(7);
            List<OMElement> unknownAssocs = m.getAssociations();
            List<OMElement> folderDocAssocs = new ArrayList<OMElement>();
            OMElement a;

            a = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(1), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociation(m.getSubmissionSet(), m.getFolder(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }


            a = hasAssociation(m.getFolder(0), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
                folderDocAssocs.add(a);
            }

            a = hasAssociation(m.getFolder(0), m.getExtrinsicObject(1), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
                folderDocAssocs.add(a);
            }

            a = hasAssociationWithOneTarget(m.getSubmissionSet(), folderDocAssocs, MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
                folderDocAssocs.remove(a);
            }

            a = hasAssociationWithOneTarget(m.getSubmissionSet(), folderDocAssocs, MetadataSupport.xdsB_eb_assoc_type_has_member);

        }
        return !hasError();
    }

    /**
     *
     * @return
     * @throws MetadataException
     */
    public boolean sswithTwoDocOneFolOneDocInFol() throws MetadataException {
        hasSubmissionSet();
        hasDocuments(2);
        hasFolders(1);
        if (!hasError()) {
            hasAssociations(5);
            List<OMElement> unknownAssocs = m.getAssociations();
            List<OMElement> folderDocAssocs = new ArrayList<OMElement>();
            OMElement a;

            a = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociation(m.getSubmissionSet(), m.getExtrinsicObject(1), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociation(m.getSubmissionSet(), m.getFolder(0), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
            }

            a = hasAssociationWithOneTarget(m.getFolder(0), m.getExtrinsicObjects(), MetadataSupport.xdsB_eb_assoc_type_has_member);
            if (a != null) {
                unknownAssocs.remove(a);
                a = this.hasAssociation(m.getSubmissionSet(), a, MetadataSupport.xdsB_eb_assoc_type_has_member);
                if (a != null) {
                    unknownAssocs.remove(a);
                }
            }


        }
        return !hasError();
    }

    static void println(String x) {
        System.out.println(x);
    }

    static void usage() {
        println("metav ");
        println("\t[-h] - print usage message and exit");
        println("\t[-af <assertion file name>]");
        println("\t[-as <assertion set name>]");
        println("\t[-teststep <test step name>]");
        println("\t\tThe input file is inspected assuming it is in the format of a log.xml file. ");
        println("\t\tTestStep <test step name> is found ");
        println("\t\tand the query result is extracted from the Result element.");
        println("\tfile");
        System.exit(-1);
    }

    /**
     *
     * @param args
     */
    static public void main(String[] args) {
        String filename = null;
        String assertion_filename = null;
        String assertion_set_name = null;
        String test_step = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-af")) {
                if (i + 1 < args.length) {
                    i++;
                    assertion_filename = args[i];
                } else {
                    usage();
                }
            } else if (args[i].equals("-h")) {
                usage();
            } else if (args[i].equals("-teststep")) {
                if (i + 1 < args.length) {
                    i++;
                    test_step = args[i];
                } else {
                    usage();
                }
            } else if (args[i].equals("-as")) {
                if (i + 1 < args.length) {
                    i++;
                    assertion_set_name = args[i];
                } else {
                    usage();
                }
            } else if (i == args.length - 1) {
                filename = args[i];
            } else {
                usage();
            }
        }

        if (assertion_filename == null
                || filename == null) {
            usage();
        }

        try {
            TestValidator v;

            v = new TestValidator(new File(assertion_filename), assertion_set_name);
            OMElement input = XMLParser.fileToOM(filename);
            if (test_step == null) {
                ArrayList<String> path = new ArrayList<String>();
                path.add("TestStep");
                path.add("StoredQueryTransaction");
                path.add("Result");
                path.add("AdhocQueryResponse");
//				path.add("RegistryObjectList");
                OMElement ele_of_focus = v.find_nested_element(input, path);
                v.run_test_assertions(ele_of_focus);
            } else {
                OMElement step = MetadataSupport.firstChildWithLocalName(input, test_step);
                if (step == null) {
                    throw new Exception("TestStep " + test_step + " not found in file " + filename);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(-1);
        }



    }

    OMElement find_nested_element(OMElement top_element, ArrayList<String> path) throws Exception {
        OMElement current_ele = top_element;

        for (String name : path) {
            OMElement ele = MetadataSupport.firstChildWithLocalName(current_ele, name);
            if (ele == null) {
                throw new Exception("find_nexted_element: Cannot find element " + name + " in path " + path);
            }
            current_ele = ele;
        }

        return current_ele;
    }
}
