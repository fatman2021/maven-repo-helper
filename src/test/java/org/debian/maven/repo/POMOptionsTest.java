/*
 * Copyright 2009 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.debian.maven.repo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public class POMOptionsTest {

    @Test
    public void testParse() {
        POMOptions options = POMOptions.parse("--has-package-version --java-lib --no-parent");
        assertTrue(options.getHasPackageVersion());
        assertTrue(options.isJavaLib());
        assertTrue(options.isNoParent());
        assertFalse(options.isIgnorePOM());
        assertFalse(options.isNoUsjVersionless());
    }

    @Test
    public void testOptionsToString() {
        POMOptions options = new POMOptions();
        options.setIgnore(true);
        options.setNoParent(true);
        
        assertEquals(" --ignore", options.toString());
        
        options.setIgnore(false);
        options.setHasPackageVersion(true);
        
        assertEquals(" --no-parent --has-package-version", options.toString());
    }
}
