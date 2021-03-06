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
package com.vangent.hieos.empi.impl.base;

import com.vangent.hieos.empi.adapter.EMPINotification;
import com.vangent.hieos.empi.config.EMPIConfig;
import com.vangent.hieos.empi.exception.EMPIException;
import com.vangent.hieos.empi.persistence.PersistenceManager;
import com.vangent.hieos.empi.persistence.EnterpriseSubjectController;
import com.vangent.hieos.subjectmodel.DeviceInfo;
import com.vangent.hieos.subjectmodel.InternalId;
import com.vangent.hieos.subjectmodel.Subject;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernie Thuman
 */
public class BaseHandler {

    private static final Logger logger = Logger.getLogger(BaseHandler.class);
    private PersistenceManager persistenceManager = null;
    private DeviceInfo senderDeviceInfo = null;

    /**
     *
     * @param persistenceManager
     * @param senderDeviceInfo
     */
    protected BaseHandler(PersistenceManager persistenceManager, DeviceInfo senderDeviceInfo) {
        this.persistenceManager = persistenceManager;
        this.senderDeviceInfo = senderDeviceInfo;
    }

    /**
     *
     * @return
     */
    public DeviceInfo getSenderDeviceInfo() {
        return senderDeviceInfo;
    }

    /**
     *
     * @return
     */
    protected PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    /**
     *
     * @param notification
     * @param enterpriseSubjectId
     * @throws EMPIException
     */
    protected void addSubjectToNotification(EMPINotification notification, InternalId enterpriseSubjectId) throws EMPIException {
        EMPIConfig empiConfig = EMPIConfig.getInstance();
        if (!empiConfig.isUpdateNotificationEnabled()) {
            // Notifications are turned off -- get out now.
            return; // Early exit!!
        }
        EnterpriseSubjectController enterpriseSubjectController = new EnterpriseSubjectController(this.getPersistenceManager());
        Subject enterpriseSubject = enterpriseSubjectController.loadSubjectIdentifiersAndNamesOnly(enterpriseSubjectId);
        notification.addSubject(enterpriseSubject);
    }
}
