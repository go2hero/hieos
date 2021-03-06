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
package com.vangent.hieos.empi.transform;

/**
 *
 * @author Bernie Thuman
 */
public class PrefixTransformFunction extends TransformFunction {

    private static String PARAM_PREFIX_LENGTH = "prefix-length";

    /**
     *
     * @param obj
     * @return
     */
    public Object transform(Object obj) {
        String s = (String) obj;
        s = s.toLowerCase();  // Convert to lower case.
        int prefixLength = this.getFunctionConfig().getParameterAsInteger(PARAM_PREFIX_LENGTH, -1);
        if (prefixLength != -1) {
            if (s.length() > prefixLength) {
                s = s.substring(0, prefixLength);
            }
        }
        return s;
    }
}
