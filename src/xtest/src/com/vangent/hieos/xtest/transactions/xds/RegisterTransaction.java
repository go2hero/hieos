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
package com.vangent.hieos.xtest.transactions.xds;

import com.vangent.hieos.xtest.framework.BasicTransaction;
import com.vangent.hieos.xtest.framework.StepContext;
import com.vangent.hieos.xtest.framework.TestConfig;
import com.vangent.hieos.xutil.metadata.structure.MetadataTypes;
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsInternalException;

import com.vangent.hieos.xutil.soap.SoapActionFactory;
import java.util.Iterator;
import org.apache.axiom.om.OMElement;

/**
 *
 * @author thumbe
 */
public class RegisterTransaction extends BasicTransaction {

    /**
     * 
     * @param s_ctx
     * @param instruction
     * @param instruction_output
     */
    public RegisterTransaction(StepContext s_ctx, OMElement instruction, OMElement instruction_output) {
        super(s_ctx, instruction, instruction_output);
        //		this.xds_version = xds_version;
    }

    /**
     *
     * @throws XdsException
     */
    public void run() throws XdsException {
        Iterator elements = instruction.getChildElements();
        while (elements.hasNext()) {
            OMElement part = (OMElement) elements.next();
            parse_instruction(part);
        }

        // Endpoint for default registry
        parseRegistryEndpoint(TestConfig.defaultRegistry, this.getTransactionName());
        validate_xds_version();

        useMtom = false;
        if (xds_version == BasicTransaction.xds_b) {
            useAddressing = true;
            soap_1_2 = true;
        } else {
            useAddressing = false;
            soap_1_2 = false;
        }
        if (metadata_filename == null) {
            throw new XdsInternalException("No MetadataFile element found for "
                    + this.getTransactionName()
                    + " instruction within step "
                    + this.s_ctx.get("step_id"));
        }

        OMElement request = prepare_metadata();
        log_metadata(request);
        setMetadata(request);
        try {
            soapCall();
            OMElement result = getSoapResult();
            if (result != null) {
                this.s_ctx.add_name_value(instruction_output, "Result", result);
                validate_registry_response(
                        result,
                        MetadataTypes.METADATA_TYPE_SQ);
            } else {
                this.s_ctx.add_name_value(instruction_output, "Result", "None");
                s_ctx.set_error("Result was null");
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     *
     * @return
     */
    protected String getTransactionName() {
        return "RegisterDocumentSet-b";
    }

    /**
     *
     * @return
     */
    protected String getRequestAction() {
        if (xds_version == BasicTransaction.xds_b) {
            return SoapActionFactory.XDSB_REGISTRY_REGISTER_ACTION;
        } else {
            return SoapActionFactory.ANON_ACTION;
        }
    }
}
