/*
 * Copyright 2011 Damien Raude-Morvan.
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

/**
 * @author Damien Raude-Morvan <drazzib@debian.org>
 */
public class RepositoryReportTextWriter implements RepositoryReportWriter {

    /**
     * Current level of item
     */
    private int itemLevel = 1;

    public void printSectionStart(String string) {
        String display = string + ":";

        System.out.println(display.replaceAll(".", "="));
        System.out.println(display);
        System.out.println(display.replaceAll(".", "="));
    }

    public void printSectionEnd() {
        System.out.println();
    }

    public void printItem(String string) {
        System.out.print("\t");
        if (itemLevel > 1) {
            System.out.print("\t");
        }
        System.out.println(string);
        itemLevel++;
    }

    public void endItem() {
        itemLevel--;
    }

    public void printStart() {
        System.out.println("Scanning repository...");
    }

    public void printEnd() {
        System.out.println("Done.");
    }

}
