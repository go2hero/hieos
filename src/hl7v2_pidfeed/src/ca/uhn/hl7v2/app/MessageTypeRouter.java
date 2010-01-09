/**
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
specific language governing rights and limitations under the License.

The Original Code is "Connection.java".  Description:
"A TCP/IP connection to a remote HL7 server."

The Initial Developer of the Original Code is University Health Network. Copyright (C)
2002.  All Rights Reserved.

Contributor(s): ______________________________________.

Alternatively, the contents of this file may be used under the terms of the
GNU General Public License (the  �GPL�), in which case the provisions of the GPL are
applicable instead of those above.  If you wish to allow use of your version of this
file only under the terms of the GPL and not to allow others to use your version
of this file under the MPL, indicate your decision by deleting  the provisions above
and replace  them with the notice and other provisions required by the GPL License.
If you do not delete the provisions above, a recipient may use your version of
this file under either the MPL or the GPL.
*/
// HIEOS - Minor modifications to pass accepted socket to message handler.
package ca.uhn.hl7v2.app;

import java.util.HashMap;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.log.HapiLog;
import ca.uhn.log.HapiLogFactory;
// +++++ HIEOS CHANGES BEGIN HERE +++++
import java.net.Socket;
import com.vangent.hieos.services.xds.registry.transactions.hl7v2.HL7Application;
// +++++ HIEOS CHANGES END HERE +++++

/**
 * Routes messages to various Applications based on message type and trigger event.
 * The router is told which Application to which to route various messages by calling
 * the method <code>registerApplication(...)</code>.
 * @author Bryan Tripp
 */
public class MessageTypeRouter implements Application {
    
    private static final HapiLog log = HapiLogFactory.getHapiLog(MessageTypeRouter.class);
    private HashMap apps;
    
    /** Creates a new instance of MessageTypeRouter */
    public MessageTypeRouter() {
        apps = new HashMap(20);
    }
    
    /**
     * Returns true if at least one application has been registered to accept this
     * type of message.  Applications are registered using <code>registerApplication(...)</code>.
     */
    public boolean canProcess(Message in) {
        boolean can = false;
        try {
            Application matches = this.getMatchingApplication(in);
            if (matches != null) can = true;
        } catch (HL7Exception e) { 
            can = false;
        }
        return can;
    }
    
    /**
     * Forwards the given message to any Applications that have been registered to
     * accept messages of that type and trigger event.
     * @throws ApplicationException if no such Applications are registered, or if
     *      the underlying Application throws this exception during processing.
     */
    public Message processMessage(Message in) throws ApplicationException {
        Message out;
        try {
            Application matchingApp = this.getMatchingApplication(in);
            out = matchingApp.processMessage(in);
        } catch (HL7Exception e) {
            throw new ApplicationException("Error internally routing message: " + e.toString(), e);
        }
        return out;
    }

    // +++++ HIEOS CHANGES BEGIN HERE +++++
    public Message processMessage(Message in, Socket socket) throws ApplicationException {
        Message out;
        try {
            Application matchingApp = this.getMatchingApplication(in);
            if (matchingApp instanceof HL7Application) {
                out = ((HL7Application)matchingApp).processMessage(in, socket);
            }
            else {
                out = matchingApp.processMessage(in);
            }
        } catch (HL7Exception e) {
            throw new ApplicationException("Error internally routing message: " + e.toString(), e);
        }
        return out;
    }
    // +++++ HIEOS CHANGES END HERE +++++
    
    /**
     * Registers the given application to handle messages corresponding to the given type
     * and trigger event.  Only one application can be registered for a given message type
     * and trigger event combination.  A repeated registration for a particular combination 
     * of type and trigger event over-writes the previous one.  Use "*" as a wildcard (e.g. 
     * registerApplication("ADT", "*", myApp) would register your app for all ADT messages).
     */
    public synchronized void registerApplication(String messageType, String triggerEvent, Application handler) {
        this.apps.put(getKey(messageType, triggerEvent), handler);

        //status message
        StringBuffer buf = new StringBuffer();
        buf.append(handler.getClass().getName());           
        buf.append(" registered to handle ");
        buf.append(messageType);
        buf.append("^");
        buf.append(triggerEvent);
        buf.append(" messages");
        log.info(buf.toString());
    }

    /** 
     * Returns the Applications that has been registered to handle 
     * messages of the type and trigger event of the given message, or null if 
     * there are none. 
     */
    private Application getMatchingApplication(Message message) throws HL7Exception {
        Terser t = new Terser(message);
        String messageType = t.get("/MSH-9-1");
        String triggerEvent = t.get("/MSH-9-2");
        return this.getMatchingApplication(messageType, triggerEvent);        
    }
    
    /**
     * Returns the Applications that has been registered to handle 
     * messages of the given type and trigger event, or null if 
     * there are none.  If there is not an exact match, wildcards 
     * ("*") are tried as well.  
     */
    private synchronized Application getMatchingApplication(String messageType, String triggerEvent) {
        Application matchingApp = null;
        Object o = this.apps.get(getKey(messageType, triggerEvent));
        if (o == null) o = this.apps.get(getKey(messageType, "*"));
        if (o == null) o = this.apps.get(getKey("*", triggerEvent));
        if (o == null) o = this.apps.get(getKey("*", "*"));        
        if (o != null) matchingApp = (Application)o;
        return matchingApp;
    }
    
    /**
     * Creates reproducible hash key. 
     */
    private String getKey(String messageType, String triggerEvent) {
        //create hash key string by concatenating type and trigger event
        return messageType + "|" + triggerEvent;
    }
}
