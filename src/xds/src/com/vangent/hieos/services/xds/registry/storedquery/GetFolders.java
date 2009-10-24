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

import java.util.ArrayList;
import org.apache.axiom.om.OMElement;

import com.vangent.hieos.xutil.exception.MetadataValidationException;
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.metadata.structure.Metadata;
import com.vangent.hieos.xutil.metadata.structure.MetadataParser;
import com.vangent.hieos.xutil.metadata.structure.SqParams;
import com.vangent.hieos.xutil.response.Response;
import com.vangent.hieos.xutil.query.StoredQuery;
import com.vangent.hieos.xutil.xlog.client.XLogMessage;

/**
 *
 * @author NIST (Adapted by Bernie Thuman).
 */
public class GetFolders extends StoredQuery {

    /**
     *
     * @param params
     * @param return_objects
     * @param response
     * @param log_message
     * @param is_secure
     * @throws MetadataValidationException
     */
    public GetFolders(SqParams params, boolean return_objects, Response response, XLogMessage log_message, boolean is_secure)
            throws MetadataValidationException {
        super(params, return_objects, response, log_message, is_secure);

        // param name, required?, multiple?, is string?, is code?, alternative
        validateQueryParam("$XDSFolderEntryUUID", true, true, true, false, "$XDSFolderUniqueId");
        validateQueryParam("$XDSFolderUniqueId", true, true, true, false, "$XDSFolderEntryUUID");
        if (this.has_validation_errors) {
            throw new MetadataValidationException("Metadata Validation error present");
        }
    }

    /**
     *
     * @return
     * @throws XdsException
     */
    public Metadata run_internal() throws XdsException {
        Metadata metadata;
        ArrayList<String> fol_uuid = get_arraylist_parm("$XDSFolderEntryUUID");
        if (fol_uuid != null) {
            // starting from uuid
            OMElement x = get_fol_by_uuid(fol_uuid);
            metadata = MetadataParser.parseNonSubmission(x);
            if (metadata.getFolders().size() == 0) {
                return metadata;
            }
        } else {
            // starting from uniqueid
            ArrayList<String> fol_uid = get_arraylist_parm("$XDSFolderUniqueId");
            OMElement x = getFolderByUID(fol_uid);
            metadata = MetadataParser.parseNonSubmission(x);
        }
        return metadata;
    }
}
